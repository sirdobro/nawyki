package cc.intmain.nawyki.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public sealed interface Schedule permits Schedule.Daily, Schedule.SelectedDays, Schedule.WeeklyGoal {
    record Daily() implements Schedule {}
    // The required first weekday makes the selection nonempty.
    record SelectedDays(DayOfWeek first, Set<DayOfWeek> others) implements Schedule {
        public SelectedDays { others = Set.copyOf(others); }
        public Set<DayOfWeek> days() {
            return Stream.concat(Stream.of(first), others.stream()).collect(Collectors.toUnmodifiableSet());
        }
    }
    enum Frequency {
        ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN;
        public int days() { return ordinal() + 1; }
    }
    record WeeklyGoal(Frequency frequency) implements Schedule {}

    default boolean availableOn(LocalDate date) {
        return switch (this) {
            case Daily ignored -> true;
            case SelectedDays selected -> selected.days().contains(date.getDayOfWeek());
            case WeeklyGoal ignored -> true;
        };
    }
}
