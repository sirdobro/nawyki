package cc.intmain.nawyki.http;

import cc.intmain.nawyki.domain.WorkoutRules;
import cc.intmain.nawyki.persistence.WorkoutRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.*;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
public class WorkoutRoutes {
    private final WorkoutRepository workouts;
    private final ObjectMapper json;
    private final Clock clock;
    public WorkoutRoutes(WorkoutRepository workouts, ObjectMapper json, Clock clock) {
        this.workouts = workouts; this.json = json; this.clock = clock;
    }
    @Bean RouterFunction<ServerResponse> workoutApi() {
        return route().GET("/api/workouts", this::list)
                .GET("/api/workouts/{id}", r -> workouts.find(account(r), id(r)).map(w -> response(200, view(w))).orElseGet(WorkoutRoutes::notFound))
                .POST("/api/workouts", r -> save(r, false))
                .PUT("/api/workouts/{id}", r -> save(r, true))
                .DELETE("/api/workouts/{id}", r -> workouts.delete(account(r), id(r)) ? response(200, Map.of("deleted", true)) : notFound())
                .filter((request, next) -> {
                    try { return next.handle(request); }
                    catch (IllegalArgumentException | java.time.DateTimeException | BadRequest | IOException invalid) {
                        return response(400, Map.of("code", "BAD_REQUEST", "message", "Niepoprawny format danych treningu."));
                    }
                }).build();
    }
    private ServerResponse list(ServerRequest request) {
        LocalDate from = LocalDate.parse(request.param("from").orElse(today().withDayOfMonth(1).toString()));
        LocalDate to = LocalDate.parse(request.param("to").orElse(today().toString()));
        if (from.isAfter(to) || from.plusYears(5).isBefore(to))
            return response(422, Map.of("code", "INVALID_RANGE", "message", "Wybierz zakres dat do pięciu lat."));
        return response(200, Map.of("workouts", workouts.list(account(request), from, to)));
    }
    private ServerResponse save(ServerRequest request, boolean update) throws IOException {
        UUID owner = account(request);
        Optional<UUID> target = update ? Optional.of(id(request)) : Optional.empty();
        if (target.isPresent() && workouts.find(owner, target.orElseThrow()).isEmpty()) return notFound();
        byte[] bytes = request.servletRequest().getInputStream().readNBytes(16385);
        if (bytes.length > 16384) throw new BadRequest();
        JsonNode body = json.reader().with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(bytes);
        if (body == null || !body.isObject()) throw new BadRequest();
        var exercises = new ArrayList<WorkoutRules.ExerciseInput>();
        for (JsonNode exercise : array(body.path("exercises"))) {
            if (!exercise.isObject()) throw new BadRequest();
            var sets = new ArrayList<WorkoutRules.SetInput>();
            for (JsonNode set : array(exercise.path("sets"))) {
                if (!set.isObject()) throw new BadRequest();
                sets.add(new WorkoutRules.SetInput(scalar(set.path("repetitions")), scalar(set.path("weightKg"))));
            }
            exercises.add(new WorkoutRules.ExerciseInput(text(exercise.path("name")), sets));
        }
        return WorkoutRules.validate(date(text(body.path("date"))), exercises, today()).fold(valid -> {
            if (target.isPresent()) {
                UUID id = target.orElseThrow();
                return workouts.replace(owner, id, valid) ? response(200, view(new WorkoutRepository.Stored(id, valid))) : notFound();
            }
            return response(201, view(workouts.create(owner, valid)));
        }, errors -> response(422, Map.of("code", "VALIDATION", "errors", errors)));
    }
    public static Map<String, Object> view(WorkoutRepository.Stored stored) {
        return Map.of("id", stored.id(), "date", stored.workout().date(), "exercises", stored.workout().exercises().stream()
                .map(e -> Map.of("name", e.name(), "sets", e.sets().stream().map(s -> Map.of("repetitions", s.repetitions(),
                        "weightKg", s.weightKg().map(w -> w.stripTrailingZeros().toPlainString()).orElse(""))).toList())).toList());
    }
    private UUID account(ServerRequest r) { return (UUID) r.servletRequest().getAttribute("accountId"); }
    private UUID id(ServerRequest r) { return UUID.fromString(r.pathVariable("id")); }
    private LocalDate today() { return LocalDate.now(clock.withZone(ZoneId.of("Europe/Warsaw"))); }
    private static Optional<LocalDate> date(String value) {
        try { return Optional.of(LocalDate.parse(value)); }
        catch (java.time.DateTimeException invalid) { return Optional.empty(); }
    }
    private static Iterable<JsonNode> array(JsonNode value) {
        if (value.isMissingNode()) return List.of();
        if (!value.isArray()) throw new BadRequest();
        return value;
    }
    private static String text(JsonNode value) {
        if (value.isMissingNode()) return "";
        if (!value.isTextual()) throw new BadRequest();
        return value.textValue();
    }
    private static String scalar(JsonNode value) {
        if (value.isMissingNode() || value.isNull()) return "";
        if (!value.isTextual() && !value.isNumber()) throw new BadRequest();
        return value.asText();
    }
    private static ServerResponse response(int status, Object body) { return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).body(body); }
    private static ServerResponse notFound() { return response(404, Map.of("code", "NOT_FOUND", "message", "Nie znaleziono treningu.")); }
    private static final class BadRequest extends RuntimeException {}
}
