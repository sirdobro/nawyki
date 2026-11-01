package cc.intmain.nawyki.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import static cc.intmain.nawyki.domain.Result.*;

public final class HabitRules {
    private HabitRules() {}
    public record Draft(String name, Schedule schedule, LocalDate startDate) {}
    public record Habit(UUID id, String name, Schedule schedule, LocalDate startDate,
                        Optional<LocalDate> archivedOn) {}

    public static Result<Draft> validate(String name, String kind, List<Integer> days,
            int target, Optional<LocalDate> start, LocalDate today) {
        var errors = new ArrayList<Violation>();
        String cleanName = name.strip();
        if (cleanName.isEmpty() || cleanName.length() > 100)
            errors.add(new Violation("name", "Nazwa musi mieć od 1 do 100 znaków."));
        if (!Set.of("DAILY", "SELECTED_DAYS", "WEEKLY_GOAL").contains(kind))
            errors.add(new Violation("scheduleType", "Wybierz poprawny harmonogram."));
        if (kind.equals("SELECTED_DAYS") && (days.isEmpty() || days.stream().anyMatch(d -> d < 1 || d > 7)))
            errors.add(new Violation("days", "Wybierz co najmniej jeden dzień tygodnia."));
        if (kind.equals("WEEKLY_GOAL") && (target < 1 || target > 7))
            errors.add(new Violation("weeklyTarget", "Cel musi wynosić od 1 do 7 dni."));
        if (start.isEmpty() || start.orElse(today).isAfter(today))
            errors.add(new Violation("startDate", "Podaj datę rozpoczęcia nie późniejszą niż dzisiaj."));
        if (!errors.isEmpty()) return new Invalid<>(errors);
        Schedule schedule = switch (kind) {
            case "SELECTED_DAYS" -> {
                var selected = days.stream().map(DayOfWeek::of).collect(Collectors.toUnmodifiableSet());
                DayOfWeek first = selected.iterator().next();
                yield new Schedule.SelectedDays(first, selected.stream().filter(d -> d != first).collect(Collectors.toUnmodifiableSet()));
            }
            case "WEEKLY_GOAL" -> new Schedule.WeeklyGoal(Schedule.Frequency.values()[target - 1]);
            default -> new Schedule.Daily();
        };
        return new Ok<>(new Draft(cleanName, schedule, start.orElse(today)));
    }

    public static Result<LocalDate> validateCompletion(Habit habit, LocalDate date, LocalDate today) {
        var errors = new ArrayList<Violation>();
        if (date.isAfter(today) || date.isBefore(habit.startDate()))
            errors.add(new Violation("date", "Data wykonania musi mieścić się między początkiem nawyku a dzisiaj."));
        if (habit.archivedOn().filter(end -> !date.isBefore(end)).isPresent())
            errors.add(new Violation("date", "Nawyk był już zarchiwizowany w tym dniu."));
        if (!habit.schedule().availableOn(date))
            errors.add(new Violation("date", "Nawyk nie jest zaplanowany na ten dzień."));
        return errors.isEmpty() ? new Ok<>(date) : new Invalid<>(errors);
    }
}
