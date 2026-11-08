package cc.intmain.nawyki.http;

import cc.intmain.nawyki.application.AccountService;
import cc.intmain.nawyki.application.JournalChanges;
import cc.intmain.nawyki.application.JournalChanges.Channel;
import cc.intmain.nawyki.application.TokenService;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.function.*;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/** Effectful single-instance SSE adapter. No journal data or unbounded event queues. */
@Configuration
public class JournalStreams {
    private final AccountService accounts;
    private final TokenService tokens;
    private final long heartbeatMillis;
    private final ConcurrentHashMap<UUID, Client> clients = new ConcurrentHashMap<>();
    private boolean stopping;
    public JournalStreams(AccountService accounts, TokenService tokens,
            @Value("${app.sse.heartbeat-millis:15000}") long heartbeatMillis) {
        this.accounts = accounts; this.tokens = tokens; this.heartbeatMillis = heartbeatMillis;
    }
    @Bean RouterFunction<ServerResponse> streamApi() {
        return route().GET("/api/events/habits", r -> subscribe(r, Channel.HABITS))
                .GET("/api/events/workouts", r -> subscribe(r, Channel.WORKOUTS)).build();
    }
    private ServerResponse subscribe(ServerRequest request, Channel channel) {
        UUID owner = (UUID) request.servletRequest().getAttribute("accountId");
        String hash = tokens.hash(WebSecurity.sessionToken(request.servletRequest()).orElseThrow());
        if (!hasCapacity(owner)) return ServerResponse.status(429).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", "Za dużo otwartych kart. Zamknij część z nich i odśwież stronę."));
        return ServerResponse.sse(builder -> {
            Client client = new Client(owner, hash, channel, builder);
            if (!register(client)) {
                try { builder.event("limit").data(Map.of("refresh", false)); }
                catch (IOException ignored) { /* Client already disconnected. */ }
                finally { builder.complete(); }
                return;
            }
            builder.onComplete(client::close).onTimeout(client::close).onError(error -> client.close());
            client.worker.start();
        }, Duration.ofMinutes(10));
    }
    private synchronized boolean hasCapacity(UUID owner) {
        return !stopping && clients.size() < 256
                && clients.values().stream().filter(c -> c.owner.equals(owner)).count() < 16;
    }
    private synchronized boolean register(Client client) {
        if (!hasCapacity(client.owner)) return false;
        clients.put(client.id, client); return true;
    }
    @TransactionalEventListener(fallbackExecution = true)
    public void changed(JournalChanges.Change change) {
        clients.values().stream().filter(c -> c.owner.equals(change.account()) && c.channel == change.channel())
                .forEach(Client::signal);
    }
    @PreDestroy public synchronized void shutdown() {
        stopping = true;
        clients.values().forEach(Client::close);
    }
    // Package-private evidence for integration tests; never exposed over HTTP.
    int connectionCount(UUID owner) { return (int) clients.values().stream().filter(c -> c.owner.equals(owner)).count(); }

    private final class Client {
        final UUID id = UUID.randomUUID();
        final UUID owner;
        final String sessionHash;
        final Channel channel;
        final ServerResponse.SseBuilder builder;
        final AtomicBoolean pending = new AtomicBoolean();
        final AtomicBoolean closed = new AtomicBoolean();
        final Semaphore wake = new Semaphore(0);
        final Thread worker;
        Client(UUID owner, String hash, Channel channel, ServerResponse.SseBuilder builder) {
            this.owner = owner; this.sessionHash = hash; this.channel = channel; this.builder = builder;
            this.worker = Thread.ofVirtual().name("journal-sse").unstarted(this::run);
        }
        void signal() { if (!closed.get() && pending.compareAndSet(false, true)) wake.release(); }
        boolean valid() {
            if (accounts.streamSessionValid(owner, sessionHash)) return true;
            try { builder.event("session-ended").data(Map.of("refresh", false)); }
            catch (IOException ignored) { /* Closing anyway. */ }
            return false;
        }
        void run() {
            try {
                if (closed.get() || !valid()) return;
                builder.retry(Duration.ofSeconds(3)).event("ready").data(Map.of("refresh", true));
                while (!closed.get()) {
                    wake.tryAcquire(heartbeatMillis, TimeUnit.MILLISECONDS);
                    if (closed.get() || !valid()) break;
                    if (pending.getAndSet(false)) builder.event("changed").data(Map.of("refresh", true));
                    else builder.comment("keepalive").send();
                }
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            catch (IOException | RuntimeException disconnected) { /* Drop failed streams; REST is the source of truth. */ }
            finally {
                close();
                builder.complete();
            }
        }
        void close() {
            if (closed.compareAndSet(false, true)) { clients.remove(id, this); worker.interrupt(); }
        }
    }
}
