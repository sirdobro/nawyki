package cc.intmain.nawyki.http;

import cc.intmain.nawyki.application.AccountService;
import cc.intmain.nawyki.domain.HabitRules;
import cc.intmain.nawyki.domain.Schedule;
import cc.intmain.nawyki.domain.WorkoutRules;
import cc.intmain.nawyki.persistence.HabitRepository;
import cc.intmain.nawyki.persistence.WorkoutRepository;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.sse.heartbeat-millis=100")
class JournalStreamsIT {
    @LocalServerPort int port;
    @Autowired AccountService accounts;
    @Autowired JournalStreams streams;
    @Autowired HabitRepository habits;
    @Autowired WorkoutRepository workouts;
    @Autowired TransactionTemplate transaction;
    @Autowired JdbcClient jdbc;
    @Autowired TestRestTemplate rest;
    private final List<Stream> opened = new ArrayList<>();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final LocalDate today = LocalDate.now(ZoneId.of("Europe/Warsaw"));
    @AfterEach void closeStreams() { opened.forEach(Stream::close); client.close(); }

    record User(AccountService.Created account, String csrfCookie, String csrf) {
        UUID id() { return account.login().accountId(); }
        String session() { return WebSecurity.COOKIE + "=" + account.login().sessionToken(); }
    }
    private User user() {
        var token = rest.getForEntity("/api/auth/csrf", Map.class);
        String cookie = token.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("__Host-nawyki-csrf=")).findFirst().orElseThrow().split(";", 2)[0];
        return new User(accounts.create(), cookie, (String) token.getBody().get("token"));
    }
    private ResponseEntity<Map> call(User user, HttpMethod method, String path, Map<String, ?> body) {
        var headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.COOKIE, user.session() + "; " + user.csrfCookie()); headers.set("X-XSRF-TOKEN", user.csrf());
        return rest.exchange(path, method, new HttpEntity<>(body, headers), Map.class);
    }
    private Map<String, Object> habitBody() {
        return Map.of("name", "Read", "scheduleType", "DAILY", "startDate", today.toString());
    }
    private Map<String, Object> workoutBody(String name) {
        return Map.of("date", today.toString(), "exercises", List.of(Map.of("name", name,
                "sets", List.of(Map.of("repetitions", 8, "weightKg", "40")))));
    }
    private Stream open(User user, String channel) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/events/" + channel))
                .header("Cookie", user.session()).header("Accept", "text/event-stream").GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) response.body().close();
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/event-stream"));
        var stream = new Stream(response.body()); opened.add(stream); stream.expect("ready"); return stream;
    }
    private static final class Stream implements AutoCloseable {
        final InputStream input;
        final BlockingQueue<String> events = new LinkedBlockingQueue<>();
        final Thread reader;
        Stream(InputStream input) {
            this.input = input;
            reader = Thread.ofVirtual().start(() -> {
                try (var lines = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String event = "", line;
                    while ((line = lines.readLine()) != null) {
                        if (line.startsWith("event:")) event = line.substring(6).strip();
                        else if (line.isEmpty() && !event.isEmpty()) { events.add(event); event = ""; }
                    }
                } catch (IOException closed) { /* Closing a network stream unblocks the reader. */ }
            });
        }
        void expect(String event) throws InterruptedException { assertEquals(event, events.poll(5, TimeUnit.SECONDS)); }
        void quiet() throws InterruptedException { assertNull(events.poll(250, TimeUnit.MILLISECONDS)); }
        @Override public void close() { try { input.close(); } catch (IOException ignored) {} reader.interrupt(); }
    }
    private void awaitConnections(User user, int expected) throws InterruptedException {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (streams.connectionCount(user.id()) != expected && System.nanoTime() < until) Thread.sleep(10);
        assertEquals(expected, streams.connectionCount(user.id()));
    }

    @Test void bothStreamsRequireAuthentication() {
        for (String channel : List.of("habits", "workouts"))
            assertEquals(401, rest.getForEntity("/api/events/" + channel, String.class).getStatusCode().value());
    }
    @Test void habitCrudAndCompletionsNotifyOnlyOwnHabitStream() throws Exception {
        var a = user(); var b = user(); var own = open(a, "habits"); var other = open(b, "habits"); var training = open(a, "workouts");
        var created = call(a, HttpMethod.POST, "/api/habits", habitBody()); assertEquals(201, created.getStatusCode().value()); own.expect("changed");
        String path = "/api/habits/" + created.getBody().get("id");
        for (HttpMethod method : List.of(HttpMethod.PUT, HttpMethod.DELETE)) {
            assertEquals(200, call(a, method, path + "/completions/" + today, Map.of()).getStatusCode().value()); own.expect("changed");
        }
        assertEquals(200, call(a, HttpMethod.PATCH, path, Map.of("name", "New name")).getStatusCode().value()); own.expect("changed");
        assertEquals(200, call(a, HttpMethod.POST, path + "/archive", Map.of()).getStatusCode().value()); own.expect("changed");
        assertEquals(422, call(a, HttpMethod.POST, "/api/habits", Map.of("name", "")).getStatusCode().value());
        own.quiet(); other.quiet(); training.quiet();
    }
    @Test void workoutCrudNotifiesOnlyOwnWorkoutStream() throws Exception {
        var a = user(); var b = user(); var own = open(a, "workouts"); var other = open(b, "workouts"); var habit = open(a, "habits");
        var created = call(a, HttpMethod.POST, "/api/workouts", workoutBody("Squat")); assertEquals(201, created.getStatusCode().value()); own.expect("changed");
        String path = "/api/workouts/" + created.getBody().get("id");
        assertEquals(200, call(a, HttpMethod.PUT, path, workoutBody("Push-up")).getStatusCode().value()); own.expect("changed");
        assertEquals(200, call(a, HttpMethod.DELETE, path, Map.of()).getStatusCode().value()); own.expect("changed");
        assertEquals(404, call(a, HttpMethod.DELETE, path, Map.of()).getStatusCode().value());
        assertEquals(422, call(a, HttpMethod.POST, "/api/workouts", Map.of()).getStatusCode().value());
        own.quiet(); other.quiet(); habit.quiet();
    }
    @Test void notificationsWaitForCommitAndBothRollbacksStaySilent() throws Exception {
        var a = user(); var habit = open(a, "habits"); var workout = open(a, "workouts");
        var draft = new HabitRules.Draft("Rollback", new Schedule.Daily(), today);
        UUID rolledBack = transaction.execute(status -> {
            var h = habits.create(a.id(), draft);
            try { habit.quiet(); } catch (InterruptedException e) { throw new IllegalStateException(e); }
            status.setRollbackOnly(); return h.id();
        });
        assertTrue(habits.find(a.id(), rolledBack).isEmpty()); habit.quiet();
        transaction.executeWithoutResult(status -> {
            workouts.create(a.id(), new WorkoutRules.Workout(today, List.of(new WorkoutRules.Exercise("Squat",
                    List.of(new WorkoutRules.SetEntry(8, Optional.empty()))))));
            status.setRollbackOnly();
        });
        workout.quiet(); assertEquals(0, workouts.count(a.id(), today, today));
        transaction.executeWithoutResult(status -> {
            habits.create(a.id(), draft);
            try { habit.quiet(); } catch (InterruptedException e) { throw new IllegalStateException(e); }
        });
        habit.expect("changed");
    }
    @Test void logoutAllClosesBothChannelsAndRejectsReconnect() throws Exception {
        var a = user(); var h = open(a, "habits"); var w = open(a, "workouts");
        accounts.logoutAll(a.id()); h.expect("session-ended"); w.expect("session-ended"); awaitConnections(a, 0);
        assertEquals(401, call(a, HttpMethod.GET, "/api/events/habits", null).getStatusCode().value());
    }
    @Test void expiredSessionsCloseWithoutHeartbeatExtendingIdleLifetime() throws Exception {
        for (String update : List.of("last_seen_at=now()-interval '8 days'", "expires_at=now()-interval '1 second'")) {
            var a = user(); var h = open(a, "habits");
            jdbc.sql("UPDATE account_session SET " + update + " WHERE account_id=:id").param("id", a.id()).update();
            h.expect("session-ended"); awaitConnections(a, 0);
        }
    }
    @Test void keyRotationAndAccountDeletionRevokeOpenStreams() throws Exception {
        var a = user(); var h = open(a, "habits");
        assertTrue(accounts.rotate(a.id(), a.account().accessKey()).isPresent()); h.expect("session-ended"); awaitConnections(a, 0);
        var b = user(); var w = open(b, "workouts");
        assertTrue(accounts.delete(b.id(), b.account().accessKey())); w.expect("session-ended"); awaitConnections(b, 0);
    }
    @Test void disconnectReleasesResourcesAndReconnectStartsWithRestResynchronization() throws Exception {
        var a = user(); var h = open(a, "habits"); assertEquals(1, streams.connectionCount(a.id()));
        h.close(); awaitConnections(a, 0);
        var created = call(a, HttpMethod.POST, "/api/habits", habitBody());
        call(a, HttpMethod.PUT, "/api/habits/" + created.getBody().get("id") + "/completions/" + today, Map.of());
        var reconnected = open(a, "habits");
        var plan = call(a, HttpMethod.GET, "/api/today", null);
        assertEquals(true, ((Map<?, ?>) ((List<?>) plan.getBody().get("habits")).getFirst()).get("completed"));
        reconnected.quiet(); // No replay queue: ready is the signal to fetch the authoritative REST state.
    }
    @Test void accountConnectionLimitDoesNotBlockAnotherAccount() throws Exception {
        var a = user(); var b = user();
        for (int i = 0; i < 16; i++) open(a, i % 2 == 0 ? "habits" : "workouts");
        assertEquals(429, call(a, HttpMethod.GET, "/api/events/habits", null).getStatusCode().value());
        open(b, "habits"); assertEquals(1, streams.connectionCount(b.id()));
    }
}
