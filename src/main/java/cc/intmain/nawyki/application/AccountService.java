package cc.intmain.nawyki.application;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AccountService {
    public record Login(UUID accountId, String sessionToken) {}
    public record Created(Login login, String accessKey) {}
    private final JdbcClient jdbc;
    private final TokenService tokens;
    private final TransactionTemplate transaction;
    private final Clock clock;
    public AccountService(JdbcClient jdbc, TokenService tokens, TransactionTemplate transaction, Clock clock) {
        this.jdbc = jdbc; this.tokens = tokens; this.transaction = transaction; this.clock = clock;
    }
    public boolean allowAttempt(String kind) {
        Instant now = clock.instant();
        Instant window = kind.equals("create") ? now.truncatedTo(ChronoUnit.HOURS) : now.truncatedTo(ChronoUnit.MINUTES);
        int limit = kind.equals("create") ? 30 : 120;
        jdbc.sql("DELETE FROM auth_rate_bucket WHERE window_start < :cutoff").param("cutoff", java.sql.Timestamp.from(now.minus(Duration.ofHours(2)))).update();
        return jdbc.sql("""
            INSERT INTO auth_rate_bucket(kind,window_start,attempts) VALUES (:kind,:window,1)
            ON CONFLICT(kind,window_start) DO UPDATE SET attempts=auth_rate_bucket.attempts+1
            WHERE auth_rate_bucket.attempts < :limit RETURNING attempts
            """).param("kind", kind).param("window", java.sql.Timestamp.from(window)).param("limit", limit).query(Integer.class).optional().isPresent();
    }
    public Created create() {
        return transaction.execute(status -> {
            UUID id = UUID.randomUUID();
            String key = tokens.generate();
            jdbc.sql("INSERT INTO account(id) VALUES (:id)").param("id", id).update();
            jdbc.sql("INSERT INTO access_key_verifier(account_id,verifier_hash) VALUES (:id,:hash)")
                    .param("id", id).param("hash", tokens.hash(key)).update();
            return new Created(issueSession(id), key);
        });
    }
    public Optional<Login> login(String key) {
        if (!tokens.validFormat(key)) return Optional.empty();
        return transaction.execute(status -> jdbc.sql("SELECT account_id FROM access_key_verifier WHERE verifier_hash=:hash FOR UPDATE")
                .param("hash", tokens.hash(key)).query(UUID.class).optional().map(this::issueSession));
    }
    public Optional<UUID> authenticate(String token) {
        if (!tokens.validFormat(token)) return Optional.empty();
        return jdbc.sql("""
            UPDATE account_session SET last_seen_at=:now
            WHERE verifier_hash=:hash AND expires_at>:now AND last_seen_at>:cutoff RETURNING account_id
            """).param("now", java.sql.Timestamp.from(clock.instant())).param("cutoff", java.sql.Timestamp.from(clock.instant().minus(Duration.ofDays(7))))
                .param("hash", tokens.hash(token)).query(UUID.class).optional();
    }
    public void logout(String token) {
        jdbc.sql("DELETE FROM account_session WHERE verifier_hash=:hash").param("hash", tokens.hash(token)).update();
    }
    public void logoutAll(UUID id) {
        jdbc.sql("DELETE FROM account_session WHERE account_id=:id").param("id", id).update();
    }
    public Optional<Created> rotate(UUID id, String currentKey) {
        return transaction.execute(status -> {
            if (!lockKey(id, currentKey)) return Optional.empty();
            String key = tokens.generate();
            jdbc.sql("UPDATE access_key_verifier SET verifier_hash=:hash,created_at=:now WHERE account_id=:id")
                    .param("hash", tokens.hash(key)).param("now", java.sql.Timestamp.from(clock.instant())).param("id", id).update();
            logoutAll(id);
            return Optional.of(new Created(issueSession(id), key));
        });
    }
    public boolean delete(UUID id, String currentKey) {
        return transaction.execute(status -> {
            if (!lockKey(id, currentKey)) return false;
            jdbc.sql("DELETE FROM account WHERE id=:id").param("id", id).update();
            return true;
        });
    }
    private boolean lockKey(UUID id, String key) {
        return tokens.validFormat(key) && jdbc.sql("SELECT account_id FROM access_key_verifier WHERE account_id=:id AND verifier_hash=:hash FOR UPDATE")
                .param("id", id).param("hash", tokens.hash(key)).query(UUID.class).optional().isPresent();
    }
    private Login issueSession(UUID id) {
        String token = tokens.generate();
        Instant now = clock.instant();
        jdbc.sql("DELETE FROM account_session WHERE expires_at<=:now OR last_seen_at<:cutoff")
                .param("now", java.sql.Timestamp.from(now)).param("cutoff", java.sql.Timestamp.from(now.minus(Duration.ofDays(7)))).update();
        jdbc.sql("INSERT INTO account_session(id,account_id,verifier_hash,created_at,expires_at,last_seen_at) VALUES (:sid,:id,:hash,:now,:expires,:now)")
                .param("sid", UUID.randomUUID()).param("id", id).param("hash", tokens.hash(token))
                .param("now", java.sql.Timestamp.from(now)).param("expires", java.sql.Timestamp.from(now.plus(Duration.ofDays(30)))).update();
        return new Login(id, token);
    }
}
