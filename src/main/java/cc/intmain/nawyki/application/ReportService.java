package cc.intmain.nawyki.application;

import cc.intmain.nawyki.domain.HabitProgress;
import cc.intmain.nawyki.persistence.HabitRepository;
import cc.intmain.nawyki.persistence.WorkoutRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReportService {
    public record HabitSummary(UUID id, String name, String scheduleType, int weeklyTarget,
                               HabitProgress.Progress progress) {}
    public record Report(LocalDate from, LocalDate to, LocalDate today, String timeZone,
                         List<HabitSummary> habits, int workoutCount) {
        public Report { habits = List.copyOf(habits); }
    }
    private final HabitRepository habits;
    private final WorkoutRepository workouts;
    private final ExecutorService executor;
    private final PlatformTransactionManager transactions;
    public ReportService(HabitRepository habits, WorkoutRepository workouts,
            @Qualifier("reportExecutor") ExecutorService executor, PlatformTransactionManager transactions) {
        this.habits = habits; this.workouts = workouts; this.executor = executor; this.transactions = transactions;
    }
    public Report report(UUID account, LocalDate from, LocalDate to, LocalDate today) {
        return ParallelReport.combine(executor, () -> habitSummaries(account, from, to, today),
                () -> workouts.count(account, from, to.isAfter(today) ? today : to),
                (h, w) -> new Report(from, to, today, "Europe/Warsaw", h, w));
    }
    private List<HabitSummary> habitSummaries(UUID account, LocalDate from, LocalDate to, LocalDate today) {
        // The habit task owns its read snapshot; no connection crosses task boundaries.
        var snapshot = new TransactionTemplate(transactions);
        snapshot.setReadOnly(true);
        snapshot.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return snapshot.execute(status -> habits.list(account).stream()
                .filter(h -> !h.startDate().isAfter(to))
                .filter(h -> h.archivedOn().filter(d -> !d.isAfter(from)).isEmpty())
                .map(h -> new HabitSummary(h.id(), h.name(), HabitRepository.type(h.schedule()),
                        h.schedule() instanceof cc.intmain.nawyki.domain.Schedule.WeeklyGoal goal ? goal.frequency().days() : 0,
                        HabitProgress.calculate(h, habits.completions(account, h.id(), h.startDate(), today), from, to, today)))
                .toList());
    }
    @Configuration
    static class Execution {
        @Bean(destroyMethod = "shutdownNow") ExecutorService reportExecutor() {
            // Bounded workers AND queue; overload returns 503 rather than growing indefinitely.
            return new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32),
                    Thread.ofPlatform().name("report-", 0).factory(), new ThreadPoolExecutor.AbortPolicy());
        }
    }
}
