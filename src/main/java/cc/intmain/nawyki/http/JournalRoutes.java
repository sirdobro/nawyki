package cc.intmain.nawyki.http;

import cc.intmain.nawyki.application.AccountService;
import cc.intmain.nawyki.domain.HabitRules;
import cc.intmain.nawyki.domain.Schedule;
import cc.intmain.nawyki.domain.Result;
import cc.intmain.nawyki.persistence.HabitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
public class JournalRoutes {
    private final AccountService accounts;
    private final HabitRepository habits;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;
    private final Clock clock;
    public JournalRoutes(AccountService accounts, HabitRepository habits, ObjectMapper json,
            TransactionTemplate transaction, Clock clock) {
        this.accounts = accounts; this.habits = habits; this.json = json; this.transaction = transaction; this.clock = clock;
    }

    @Bean RouterFunction<ServerResponse> journalApi() {
        return route()
                .GET("/api/auth/csrf", r -> {
                    CsrfToken token = (CsrfToken) r.servletRequest().getAttribute(CsrfToken.class.getName());
                    return ok(Map.of("token", token.getToken(), "headerName", token.getHeaderName()));
                })
                .GET("/api/auth/me", r -> account(r).map(id -> ok(Map.of("authenticated", true, "accountId", id,
                        "today", today(), "timeZone", "Europe/Warsaw"))).orElseGet(() -> ok(Map.of("authenticated", false))))
                .POST("/api/accounts", this::createAccount)
                .POST("/api/auth/login", this::login)
                .POST("/api/auth/logout", r -> {
                    WebSecurity.sessionToken(r.servletRequest()).ifPresent(accounts::logout);
                    return signedOut();
                })
                .POST("/api/auth/logout-all", r -> { accounts.logoutAll(requiredAccount(r)); return signedOut(); })
                .POST("/api/auth/rotate", this::rotateKey)
                .DELETE("/api/account", this::deleteAccount)
                .GET("/api/account/export", this::exportAccount)
                .GET("/api/habits", r -> ok(Map.of("habits", habits.list(requiredAccount(r)).stream().map(this::view).toList())))
                .POST("/api/habits", this::createHabit)
                .GET("/api/habits/{id}", r -> owned(r).map(h -> ok(view(h))).orElseGet(JournalRoutes::notFound))
                .PATCH("/api/habits/{id}", this::renameHabit)
                .POST("/api/habits/{id}/archive", this::archiveHabit)
                .GET("/api/habits/{id}/completions", this::history)
                .PUT("/api/habits/{id}/completions/{date}", r -> completion(r, true))
                .DELETE("/api/habits/{id}/completions/{date}", r -> completion(r, false))
                .GET("/api/today", this::dayPlan)
                .filter((request, next) -> {
                    try { return next.handle(request); }
                    catch (BadRequest | IllegalArgumentException | java.time.DateTimeException invalid) { return error(400, "BAD_REQUEST", "Niepoprawny format danych."); }
                    catch (IOException unreadable) { return error(400, "BAD_REQUEST", "Nie udało się odczytać żądania."); }
                })
                .build();
    }

    private ServerResponse createAccount(ServerRequest request) {
        if (account(request).isPresent()) return error(409, "ALREADY_SIGNED_IN", "Najpierw wyloguj się z obecnego dziennika.");
        if (!accounts.allowAttempt("create")) return limited();
        AccountService.Created created = accounts.create();
        return sessionResponse(201, created.login(), Map.of("accountId", created.login().accountId(), "accessKey", created.accessKey()));
    }
    private ServerResponse login(ServerRequest request) throws IOException {
        if (!accounts.allowAttempt("login")) return limited();
        Map<String, Object> body = body(request);
        Optional<AccountService.Login> login = accounts.login(text(body, "accessKey").strip());
        if (login.isEmpty()) return error(401, "INVALID_KEY", "Niepoprawny klucz dostępu.");
        var authenticated = login.orElseThrow();
        if (account(request).filter(id -> !id.equals(authenticated.accountId())).isPresent()
                && !Boolean.TRUE.equals(body.get("confirmSwitch"))) {
            accounts.logout(authenticated.sessionToken());
            return error(409, "CONFIRM_SWITCH", "Potwierdź przejście do innego dziennika.");
        }
        WebSecurity.sessionToken(request.servletRequest()).ifPresent(accounts::logout);
        return sessionResponse(200, authenticated, Map.of("accountId", authenticated.accountId()));
    }
    private ServerResponse rotateKey(ServerRequest request) throws IOException {
        UUID account = requiredAccount(request);
        if (!accounts.allowAttempt("login")) return limited();
        return accounts.rotate(account, text(body(request), "accessKey").strip())
                .map(rotated -> sessionResponse(200, rotated.login(), Map.of("accessKey", rotated.accessKey())))
                .orElseGet(() -> error(401, "INVALID_KEY", "Niepoprawny klucz dostępu."));
    }
    private ServerResponse deleteAccount(ServerRequest request) throws IOException {
        UUID account = requiredAccount(request);
        if (!accounts.allowAttempt("login")) return limited();
        if (!accounts.delete(account, text(body(request), "accessKey").strip())) return error(401, "INVALID_KEY", "Niepoprawny klucz dostępu.");
        return signedOut();
    }
    private ServerResponse exportAccount(ServerRequest request) {
        UUID account = requiredAccount(request);
        var exported = habits.list(account).stream().map(h -> {
            var data = new LinkedHashMap<>(view(h));
            data.put("completions", habits.completions(account, h.id(), h.startDate(), today()));
            return data;
        }).toList();
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nawyki-export.json\"")
                .body(Map.of("formatVersion", 1, "timeZone", "Europe/Warsaw", "habits", exported));
    }
    private ServerResponse createHabit(ServerRequest request) throws IOException {
        UUID account = requiredAccount(request);
        var body = body(request);
        var days = integerList(body.get("days"));
        Optional<LocalDate> start = parseOptionalDate(text(body, "startDate"));
        return HabitRules.validate(text(body, "name"), text(body, "scheduleType"), days,
                integer(body.getOrDefault("weeklyTarget", 0)), start, today()).fold(
                        draft -> ServerResponse.status(201).contentType(MediaType.APPLICATION_JSON)
                                .body(view(habits.create(account, draft))), JournalRoutes::validation);
    }
    private ServerResponse renameHabit(ServerRequest request) throws IOException {
        var habit = owned(request);
        if (habit.isEmpty()) return notFound();
        String name = text(body(request), "name").strip();
        if (name.isEmpty() || name.length() > 100) return validation(List.of(new Result.Violation("name", "Nazwa musi mieć od 1 do 100 znaków.")));
        habits.rename(requiredAccount(request), habit.orElseThrow().id(), name);
        return ok(Map.of("saved", true));
    }
    private ServerResponse archiveHabit(ServerRequest request) {
        UUID account = requiredAccount(request);
        UUID id = UUID.fromString(request.pathVariable("id"));
        return transaction.execute(status -> {
            habits.lock(account, id);
            if (habits.find(account, id).isEmpty()) return notFound();
            habits.archive(account, id, today());
            return ok(Map.of("archived", true));
        });
    }
    private ServerResponse completion(ServerRequest request, boolean complete) {
        UUID account = requiredAccount(request);
        UUID id = UUID.fromString(request.pathVariable("id"));
        LocalDate date = LocalDate.parse(request.pathVariable("date"));
        return transaction.execute(status -> {
            habits.lock(account, id);
            var habit = habits.find(account, id);
            if (habit.isEmpty()) return notFound();
            return HabitRules.validateCompletion(habit.orElseThrow(), date, today()).fold(validDate -> {
                if (complete) habits.complete(account, id, validDate); else habits.undo(account, id, validDate);
                return ok(Map.of("completed", complete, "date", date));
            }, JournalRoutes::validation);
        });
    }
    private ServerResponse history(ServerRequest request) {
        UUID account = requiredAccount(request);
        var habit = owned(request);
        if (habit.isEmpty()) return notFound();
        LocalDate from = LocalDate.parse(request.param("from").orElse(today().minusMonths(1).toString()));
        LocalDate to = LocalDate.parse(request.param("to").orElse(today().toString()));
        if (from.isAfter(to) || from.plusYears(5).isBefore(to)) return error(422, "INVALID_RANGE", "Wybierz zakres do pięciu lat.");
        return ok(Map.of("dates", habits.completions(account, habit.orElseThrow().id(), from, to)));
    }
    private ServerResponse dayPlan(ServerRequest request) {
        UUID account = requiredAccount(request);
        LocalDate date = LocalDate.parse(request.param("date").orElse(today().toString()));
        if (date.isAfter(today())) return error(422, "FUTURE_DATE", "Nie można oznaczać przyszłych dni.");
        var items = habits.list(account).stream().filter(h -> !date.isBefore(h.startDate()))
                .filter(h -> h.archivedOn().filter(end -> !date.isBefore(end)).isEmpty())
                .filter(h -> h.schedule().availableOn(date)).map(h -> {
                    var item = new LinkedHashMap<>(view(h));
                    item.put("completed", !habits.completions(account, h.id(), date, date).isEmpty());
                    LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    item.put("weekCount", habits.completions(account, h.id(), monday, monday.plusDays(6)).size());
                    return item;
                }).toList();
        return ok(Map.of("date", date, "today", today(), "habits", items));
    }
    private Map<String, Object> view(HabitRules.Habit habit) {
        var data = new LinkedHashMap<String, Object>();
        data.put("id", habit.id()); data.put("name", habit.name());
        data.put("scheduleType", HabitRepository.type(habit.schedule())); data.put("startDate", habit.startDate());
        data.put("archivedOn", habit.archivedOn().map(LocalDate::toString).orElse(""));
        data.put("days", habit.schedule() instanceof Schedule.SelectedDays days ? days.days().stream().map(DayOfWeek::getValue).sorted().toList() : List.of());
        data.put("weeklyTarget", habit.schedule() instanceof Schedule.WeeklyGoal goal ? goal.frequency().days() : 0);
        return data;
    }
    private Optional<HabitRules.Habit> owned(ServerRequest r) { return habits.find(requiredAccount(r), UUID.fromString(r.pathVariable("id"))); }
    private Optional<UUID> account(ServerRequest r) { return Optional.ofNullable((UUID) r.servletRequest().getAttribute("accountId")); }
    private UUID requiredAccount(ServerRequest r) { return account(r).orElseThrow(); }
    private LocalDate today() { return LocalDate.now(clock.withZone(ZoneId.of("Europe/Warsaw"))); }
    private ServerResponse sessionResponse(int code, AccountService.Login login, Map<String, ?> data) {
        return ServerResponse.status(code).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.SET_COOKIE, cookie(login.sessionToken(), 30 * 24 * 60 * 60)).body(data);
    }
    private ServerResponse signedOut() {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.SET_COOKIE, cookie("", 0)).body(Map.of("signedOut", true));
    }
    private String cookie(String token, long maxAge) {
        return ResponseCookie.from(WebSecurity.COOKIE, token).secure(true).httpOnly(true).sameSite("Strict")
                .path("/").maxAge(maxAge).build().toString();
    }
    @SuppressWarnings("unchecked") private Map<String, Object> body(ServerRequest request) throws IOException {
        byte[] input = request.servletRequest().getInputStream().readNBytes(16385);
        if (input.length > 16384) throw new BadRequest();
        Object parsed = json.readValue(input, Object.class);
        if (!(parsed instanceof Map<?, ?> map)) throw new BadRequest();
        return (Map<String, Object>) map;
    }
    private static String text(Map<String, Object> body, String name) {
        Object value = body.get(name);
        if (value == null) return "";
        if (!(value instanceof String text)) throw new BadRequest();
        return text;
    }
    private static int integer(Object value) { if (value instanceof Integer n) return n; throw new BadRequest(); }
    private static List<Integer> integerList(Object value) {
        if (value == null) return List.of();
        if (!(value instanceof List<?> list)) throw new BadRequest();
        return list.stream().map(JournalRoutes::integer).toList();
    }
    private static Optional<LocalDate> parseOptionalDate(String value) {
        try { return value.isEmpty() ? Optional.empty() : Optional.of(LocalDate.parse(value)); }
        catch (java.time.DateTimeException invalid) { return Optional.empty(); }
    }
    private static ServerResponse ok(Object value) { return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(value); }
    private static ServerResponse error(int status, String code, String message) { return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).body(Map.of("code", code, "message", message)); }
    private static ServerResponse notFound() { return error(404, "NOT_FOUND", "Nie znaleziono nawyku."); }
    private static ServerResponse limited() { return error(429, "RATE_LIMITED", "Za dużo prób. Spróbuj ponownie później."); }
    private static ServerResponse validation(List<Result.Violation> errors) { return ServerResponse.status(422).contentType(MediaType.APPLICATION_JSON).body(Map.of("code", "VALIDATION", "errors", errors)); }
    private static final class BadRequest extends RuntimeException {}
}
