package cc.intmain.nawyki.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Pure calculations. A weekly period is anchored to the habit's start date. */
public final class HabitProgress {
    private HabitProgress() {}
    public enum Status { MET, MISSED, IN_PROGRESS, PARTIAL }
    public record Period(LocalDate from, LocalDate to, int completed, int target, Status status) {}
    public record Streak(int current, int best, String unit, boolean archived) {}
    public record Progress(int completed, int scheduled, List<Period> periods, Streak streak) {
        public Progress { periods = List.copyOf(periods); }
    }

    public static LocalDate weekStart(LocalDate start, LocalDate date) {
        return start.plusDays(Math.floorDiv(ChronoUnit.DAYS.between(start, date), 7) * 7);
    }

    public static Progress calculate(HabitRules.Habit habit, Collection<LocalDate> history,
                                     LocalDate from, LocalDate to, LocalDate today) {
        LocalDate end = habit.archivedOn().map(d -> d.minusDays(1)).filter(d -> d.isBefore(today)).orElse(today);
        Set<LocalDate> valid = history.stream().filter(d -> !d.isBefore(habit.startDate()) && !d.isAfter(end))
                .filter(habit.schedule()::availableOn).collect(Collectors.toUnmodifiableSet());
        boolean weekly = habit.schedule() instanceof Schedule.WeeklyGoal;
        List<Period> all;
        if (end.isBefore(habit.startDate())) all = List.of();
        else if (habit.schedule() instanceof Schedule.WeeklyGoal goal) {
            var counts = valid.stream().collect(Collectors.groupingBy(d -> weekStart(habit.startDate(), d), Collectors.counting()));
            long weeks = ChronoUnit.DAYS.between(habit.startDate(), end) / 7 + 1;
            all = java.util.stream.LongStream.range(0, weeks).mapToObj(i -> {
                LocalDate first = habit.startDate().plusWeeks(i), last = first.plusDays(6);
                int count = counts.getOrDefault(first, 0L).intValue();
                boolean archived = habit.archivedOn().filter(d -> !d.isAfter(last) && !d.isAfter(today)).isPresent();
                Status status = count >= goal.frequency().days() ? Status.MET
                        : archived ? Status.PARTIAL : !last.isBefore(today) ? Status.IN_PROGRESS : Status.MISSED;
                return new Period(first, last, count, goal.frequency().days(), status);
            }).toList();
        } else {
            all = habit.startDate().datesUntil(end.plusDays(1)).filter(habit.schedule()::availableOn)
                    .map(d -> new Period(d, d, valid.contains(d) ? 1 : 0, 1,
                            valid.contains(d) ? Status.MET : d.equals(today) ? Status.IN_PROGRESS : Status.MISSED)).toList();
        }
        int current = 0, best = 0;
        for (Period period : all) {
            if (period.status() == Status.MET) { current++; best = Math.max(best, current); }
            else if (period.status() == Status.MISSED) current = 0;
        }
        var selected = all.stream().filter(p -> !p.to().isBefore(from) && !p.from().isAfter(to)).toList();
        int completed = (int) valid.stream().filter(d -> !d.isBefore(from) && !d.isAfter(to)).count();
        return new Progress(completed, weekly ? 0 : selected.size(), selected,
                new Streak(current, best, weekly ? "WEEKS" : "OCCURRENCES",
                        habit.archivedOn().filter(d -> !d.isAfter(today)).isPresent()));
    }
}
