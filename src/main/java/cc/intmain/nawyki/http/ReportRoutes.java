package cc.intmain.nawyki.http;

import cc.intmain.nawyki.application.ReportService;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.*;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
public class ReportRoutes {
    @Bean RouterFunction<ServerResponse> reportApi(ReportService reports, Clock clock) {
        return route().GET("/api/reports", request -> {
            try {
                LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("Europe/Warsaw")));
                LocalDate date = LocalDate.parse(request.param("date").orElse(today.toString()));
                String period = request.param("period").orElse("week");
                if (!period.equals("week") && !period.equals("month")) return error(422, "Wybierz tydzień lub miesiąc.");
                if (date.isAfter(today)) return error(422, "Wybierz datę nie późniejszą niż dzisiaj.");
                LocalDate from = period.equals("week") ? date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : date.withDayOfMonth(1);
                LocalDate to = period.equals("week") ? from.plusDays(6) : from.with(TemporalAdjusters.lastDayOfMonth());
                UUID account = (UUID) request.servletRequest().getAttribute("accountId");
                return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(reports.report(account, from, to, today));
            } catch (DateTimeException invalid) { return error(400, "Niepoprawna data."); }
            catch (RejectedExecutionException | IllegalStateException unavailable) {
                return error(503, "Podsumowanie jest chwilowo niedostępne. Spróbuj ponownie.");
            }
        }).build();
    }
    private static ServerResponse error(int status, String message) {
        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).body(Map.of("message", message));
    }
}
