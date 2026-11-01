package cc.intmain.nawyki.http;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JournalApiIT {
    @Autowired TestRestTemplate http;
    @Autowired JdbcClient jdbc;
    record Client(String csrfCookie, String csrf, String session) {
        Client withSession(String value) { return new Client(csrfCookie, csrf, value); }
    }
    record Account(Client client, String id, String key) {}
    private final LocalDate today = LocalDate.now(ZoneId.of("Europe/Warsaw"));
    @BeforeEach void resetLimiter() { jdbc.sql("DELETE FROM auth_rate_bucket").update(); }
    private String cookie(ResponseEntity<?> response, String name) {
        return response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream().filter(s -> s.startsWith(name + "=")).findFirst().orElseThrow().split(";", 2)[0];
    }
    private Client guest() {
        var response = http.getForEntity("/api/auth/csrf", Map.class);
        assertEquals(200, response.getStatusCode().value());
        return new Client(cookie(response, "__Host-nawyki-csrf"), (String) response.getBody().get("token"), "");
    }
    private ResponseEntity<Map> call(Client client, HttpMethod method, String path, Map<String, ?> body) {
        var headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.COOKIE, client.csrfCookie() + (client.session().isEmpty() ? "" : "; " + client.session()));
        headers.set("X-XSRF-TOKEN", client.csrf());
        return http.exchange(path, method, new HttpEntity<>(body, headers), Map.class);
    }
    private Account create() {
        var guest = guest(); var response = call(guest, HttpMethod.POST, "/api/accounts", Map.of());
        assertEquals(201, response.getStatusCode().value());
        String session = cookie(response, WebSecurity.COOKIE);
        assertTrue(response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream().anyMatch(c -> c.startsWith(WebSecurity.COOKIE) && c.contains("Secure") && c.contains("HttpOnly") && c.contains("SameSite=Strict") && c.contains("Path=/")));
        return new Account(guest.withSession(session), response.getBody().get("accountId").toString(), (String) response.getBody().get("accessKey"));
    }
    private String habit(Client c, String type, List<Integer> days, int target) {
        var response = call(c, HttpMethod.POST, "/api/habits", Map.of("name", "Read", "scheduleType", type, "days", days, "weeklyTarget", target, "startDate", today.minusDays(40).toString()));
        assertEquals(201, response.getStatusCode().value()); return response.getBody().get("id").toString();
    }
    @Test void csrfAndForeignOriginProtectAccountCreation() {
        assertEquals(403, http.postForEntity("/api/accounts", Map.of(), Map.class).getStatusCode().value());
        Client c = guest(); var h = new HttpHeaders(); h.set(HttpHeaders.COOKIE, c.csrfCookie()); h.set("X-XSRF-TOKEN", c.csrf()); h.setOrigin("https://other.invalid");
        assertEquals(403, http.exchange("/api/accounts", HttpMethod.POST, new HttpEntity<>(Map.of(), h), Map.class).getStatusCode().value());
        assertEquals(401, http.getForEntity("/api/habits", Map.class).getStatusCode().value());
    }
    @Test void keysAreHashedLoginRequiresSwitchConfirmationAndLogoutRevokesSession() {
        var a = create(); var b = create();
        String stored = jdbc.sql("SELECT verifier_hash FROM access_key_verifier WHERE account_id=CAST(:id AS uuid)").param("id", a.id()).query(String.class).single();
        assertEquals(43, a.key().length()); assertEquals(64, stored.length()); assertNotEquals(a.key(), stored);
        assertEquals(401, call(guest(), HttpMethod.POST, "/api/auth/login", Map.of("accessKey", "bad")).getStatusCode().value());
        assertEquals(409, call(a.client(), HttpMethod.POST, "/api/auth/login", Map.of("accessKey", b.key())).getStatusCode().value());
        var login = call(a.client(), HttpMethod.POST, "/api/auth/login", Map.of("accessKey", b.key(), "confirmSwitch", true));
        assertEquals(200, login.getStatusCode().value());
        assertEquals(false, call(a.client(), HttpMethod.GET, "/api/auth/me", null).getBody().get("authenticated"));
        var switched = a.client().withSession(cookie(login, WebSecurity.COOKIE));
        assertEquals(b.id(), call(switched, HttpMethod.GET, "/api/auth/me", null).getBody().get("accountId"));
        assertEquals(200, call(switched, HttpMethod.POST, "/api/auth/logout", Map.of()).getStatusCode().value());
        assertEquals(401, call(switched, HttpMethod.GET, "/api/habits", null).getStatusCode().value());
    }
    @Test void everyHabitOperationIsScopedToItsAccount() {
        var a = create(); var b = create(); String id = habit(a.client(), "DAILY", List.of(), 0);
        String path = "/api/habits/" + id;
        assertEquals(List.of(), call(b.client(), HttpMethod.GET, "/api/habits", null).getBody().get("habits"));
        assertEquals(404, call(b.client(), HttpMethod.GET, path, null).getStatusCode().value());
        assertEquals(404, call(b.client(), HttpMethod.PATCH, path, Map.of("name", "stolen")).getStatusCode().value());
        assertEquals(404, call(b.client(), HttpMethod.POST, path + "/archive", Map.of()).getStatusCode().value());
        assertEquals(404, call(b.client(), HttpMethod.GET, path + "/completions", null).getStatusCode().value());
        assertEquals(404, call(b.client(), HttpMethod.PUT, path + "/completions/" + today, Map.of()).getStatusCode().value());
        assertEquals(404, call(b.client(), HttpMethod.DELETE, path + "/completions/" + today, Map.of()).getStatusCode().value());
        assertEquals(List.of(), call(b.client(), HttpMethod.GET, "/api/account/export", null).getBody().get("habits"));
        assertEquals("Read", call(a.client(), HttpMethod.GET, path, null).getBody().get("name"));
    }
    @Test void schedulesValidationHistoryArchiveAndExportWork() {
        var a = create(); Client c = a.client();
        var invalid = call(c, HttpMethod.POST, "/api/habits", Map.of("name", " ", "scheduleType", "WEEKLY_GOAL", "weeklyTarget", 9, "startDate", "bad"));
        assertEquals(422, invalid.getStatusCode().value()); assertEquals(3, ((List<?>) invalid.getBody().get("errors")).size());
        assertEquals(400, call(c, HttpMethod.POST, "/api/habits", Map.of("name", 123)).getStatusCode().value());
        String daily = habit(c, "DAILY", List.of(), 0), weekly = habit(c, "WEEKLY_GOAL", List.of(), 3);
        int otherDay = today.getDayOfWeek().getValue() % 7 + 1;
        String selected = habit(c, "SELECTED_DAYS", List.of(otherDay), 0);
        assertEquals(422, call(c, HttpMethod.PUT, "/api/habits/" + selected + "/completions/" + today, Map.of()).getStatusCode().value());
        assertEquals(422, call(c, HttpMethod.PUT, "/api/habits/" + daily + "/completions/" + today.plusDays(1), Map.of()).getStatusCode().value());
        assertEquals(422, call(c, HttpMethod.PUT, "/api/habits/" + daily + "/completions/" + today.minusDays(41), Map.of()).getStatusCode().value());
        for (String id : List.of(daily, weekly)) assertEquals(200, call(c, HttpMethod.PUT, "/api/habits/" + id + "/completions/" + today.minusDays(1), Map.of()).getStatusCode().value());
        assertEquals(List.of(today.minusDays(1).toString()), call(c, HttpMethod.GET, "/api/habits/" + daily + "/completions", null).getBody().get("dates"));
        assertEquals(200, call(c, HttpMethod.DELETE, "/api/habits/" + daily + "/completions/" + today.minusDays(1), Map.of()).getStatusCode().value());
        assertEquals(200, call(c, HttpMethod.PATCH, "/api/habits/" + daily, Map.of("name", " Read more ")).getStatusCode().value());
        assertEquals("Read more", call(c, HttpMethod.GET, "/api/habits/" + daily, null).getBody().get("name"));
        assertEquals(200, call(c, HttpMethod.POST, "/api/habits/" + daily + "/archive", Map.of()).getStatusCode().value());
        assertEquals(422, call(c, HttpMethod.PUT, "/api/habits/" + daily + "/completions/" + today, Map.of()).getStatusCode().value());
        assertEquals(200, call(c, HttpMethod.PUT, "/api/habits/" + daily + "/completions/" + today.minusDays(1), Map.of()).getStatusCode().value());
        var export = call(c, HttpMethod.GET, "/api/account/export", null);
        assertEquals(3, ((List<?>) export.getBody().get("habits")).size());
        assertFalse(export.getBody().toString().contains(a.key()));
        assertEquals(200, call(c, HttpMethod.GET, "/api/today", null).getStatusCode().value());
    }
    @Test void concurrentDuplicateCompletionsAreIdempotent() throws Exception {
        var a = create(); String id = habit(a.client(), "DAILY", List.of(), 0);
        try (var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
            var results = tasks.invokeAll(java.util.stream.IntStream.range(0, 8).<Callable<Integer>>mapToObj(i -> () -> call(a.client(), HttpMethod.PUT, "/api/habits/" + id + "/completions/" + today, Map.of()).getStatusCode().value()).toList());
            for (var result : results) assertEquals(200, result.get());
        }
        assertEquals(1L, jdbc.sql("SELECT count(*) FROM habit_completion WHERE habit_id=CAST(:id AS uuid)").param("id", id).query(Long.class).single());
    }
    @Test void rotationRevokesOldKeyAndAllSessionsAndDeletionCascades() {
        var a = create(); String id = habit(a.client(), "DAILY", List.of(), 0);
        var second = call(guest(), HttpMethod.POST, "/api/auth/login", Map.of("accessKey", a.key()));
        Client secondClient = guest().withSession(cookie(second, WebSecurity.COOKIE));
        assertEquals(401, call(a.client(), HttpMethod.POST, "/api/auth/rotate", Map.of("accessKey", "bad")).getStatusCode().value());
        var rotated = call(a.client(), HttpMethod.POST, "/api/auth/rotate", Map.of("accessKey", a.key()));
        assertEquals(200, rotated.getStatusCode().value());
        assertEquals(401, call(secondClient, HttpMethod.GET, "/api/habits", null).getStatusCode().value());
        assertEquals(401, call(a.client(), HttpMethod.GET, "/api/habits", null).getStatusCode().value());
        assertEquals(401, call(guest(), HttpMethod.POST, "/api/auth/login", Map.of("accessKey", a.key())).getStatusCode().value());
        Client fresh = a.client().withSession(cookie(rotated, WebSecurity.COOKIE));
        String newKey = (String) rotated.getBody().get("accessKey"); assertNotEquals(a.key(), newKey);
        assertEquals(401, call(fresh, HttpMethod.DELETE, "/api/account", Map.of("accessKey", a.key())).getStatusCode().value());
        assertEquals(200, call(fresh, HttpMethod.DELETE, "/api/account", Map.of("accessKey", newKey)).getStatusCode().value());
        assertEquals(0L, jdbc.sql("SELECT count(*) FROM habit WHERE id=CAST(:id AS uuid)").param("id", id).query(Long.class).single());
        assertEquals(401, call(guest(), HttpMethod.POST, "/api/auth/login", Map.of("accessKey", newKey)).getStatusCode().value());
    }
    @Test void idleExpiryAbsoluteExpiryAndLogoutAllInvalidateSessions() {
        var a = create();
        jdbc.sql("UPDATE account_session SET last_seen_at=now()-interval '8 days' WHERE account_id=CAST(:id AS uuid)").param("id", a.id()).update();
        assertEquals(false, call(a.client(), HttpMethod.GET, "/api/auth/me", null).getBody().get("authenticated"));
        var login = call(guest(), HttpMethod.POST, "/api/auth/login", Map.of("accessKey", a.key()));
        Client fresh = guest().withSession(cookie(login, WebSecurity.COOKIE));
        jdbc.sql("UPDATE account_session SET created_at=now()-interval '31 days',expires_at=now()-interval '1 second' WHERE account_id=CAST(:id AS uuid)").param("id", a.id()).update();
        assertEquals(401, call(fresh, HttpMethod.GET, "/api/habits", null).getStatusCode().value());
        var another = call(guest(), HttpMethod.POST, "/api/auth/login", Map.of("accessKey", a.key()));
        Client live = guest().withSession(cookie(another, WebSecurity.COOKIE));
        assertEquals(200, call(live, HttpMethod.POST, "/api/auth/logout-all", Map.of()).getStatusCode().value());
        assertEquals(401, call(live, HttpMethod.GET, "/api/habits", null).getStatusCode().value());
    }
    @Test void creationRateLimitIsEnforced() {
        var c = guest();
        jdbc.sql("INSERT INTO auth_rate_bucket(kind,window_start,attempts) VALUES ('create',date_trunc('hour',now()),30)").update();
        assertEquals(429, call(c, HttpMethod.POST, "/api/accounts", Map.of()).getStatusCode().value());
    }
}
