package cc.intmain.nawyki.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HabitRulesTest {
    private final LocalDate today = LocalDate.of(2026, 11, 2);
    private Result<HabitRules.Draft> draft(String name, String type, List<Integer> days, int target, Optional<LocalDate> start) {
        return HabitRules.validate(name, type, days, target, start, today);
    }
    @Test void accumulatesIndependentErrorsAndCopiesErrorList() {
        var result = draft(" ", "WEEKLY_GOAL", List.of(), 8, Optional.empty());
        assertEquals(List.of("name", "weeklyTarget", "startDate"), result.fold(d -> List.of(), e -> e.stream().map(Result.Violation::field).toList()));
        var source = new ArrayList<Result.Violation>(); source.add(new Result.Violation("x", "x"));
        var invalid = new Result.Invalid<String>(source); source.clear();
        assertEquals(1, invalid.errors().size());
        assertThrows(UnsupportedOperationException.class, () -> invalid.errors().clear());
    }
    @Test void validatesAndNormalizesAllThreeScheduleVariants() {
        assertEquals("Read", draft(" Read ", "DAILY", List.of(), 0, Optional.of(today)).fold(HabitRules.Draft::name, e -> "failed"));
        var selected = draft("Read", "SELECTED_DAYS", List.of(1, 3, 1), 0, Optional.of(today)).fold(HabitRules.Draft::schedule, e -> new Schedule.Daily());
        assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), ((Schedule.SelectedDays) selected).days());
        for (int n = 1; n <= 7; n++) {
            var weekly = draft("Read", "WEEKLY_GOAL", List.of(), n, Optional.of(today)).fold(HabitRules.Draft::schedule, e -> new Schedule.Daily());
            assertEquals(n, ((Schedule.WeeklyGoal) weekly).frequency().days());
            assertTrue(weekly.availableOn(today));
        }
    }
    @Test void rejectsInvalidNamesSchedulesAndDates() {
        assertInstanceOf(Result.Invalid.class, draft("x".repeat(101), "bad", List.of(), 0, Optional.of(today.plusDays(1))));
        assertInstanceOf(Result.Invalid.class, draft("Read", "SELECTED_DAYS", List.of(), 0, Optional.of(today)));
        assertInstanceOf(Result.Invalid.class, draft("Read", "SELECTED_DAYS", List.of(0, 8), 0, Optional.of(today)));
        assertInstanceOf(Result.Invalid.class, draft("Read", "WEEKLY_GOAL", List.of(), 0, Optional.of(today)));
    }
    @Test void selectedDaysAreImmutableAndOnlyScheduledDaysAreAvailable() {
        var others = new HashSet<>(Set.of(DayOfWeek.WEDNESDAY));
        var selected = new Schedule.SelectedDays(DayOfWeek.MONDAY, others); others.clear();
        assertTrue(selected.availableOn(today)); assertFalse(selected.availableOn(today.plusDays(1)));
        assertEquals(2, selected.days().size());
        assertThrows(UnsupportedOperationException.class, () -> selected.days().clear());
    }
    @Test void completionRespectsStartTodayScheduleAndExclusiveArchiveDate() {
        var habit = new HabitRules.Habit(UUID.randomUUID(), "Read", new Schedule.Daily(), today.minusDays(4), Optional.empty());
        assertInstanceOf(Result.Ok.class, HabitRules.validateCompletion(habit, today, today));
        assertInstanceOf(Result.Ok.class, HabitRules.validateCompletion(habit, habit.startDate(), today));
        assertInstanceOf(Result.Invalid.class, HabitRules.validateCompletion(habit, today.plusDays(1), today));
        assertInstanceOf(Result.Invalid.class, HabitRules.validateCompletion(habit, habit.startDate().minusDays(1), today));
        var archived = new HabitRules.Habit(habit.id(), habit.name(), new Schedule.SelectedDays(DayOfWeek.SUNDAY, Set.of()), habit.startDate(), Optional.of(today));
        assertInstanceOf(Result.Ok.class, HabitRules.validateCompletion(archived, today.minusDays(1), today));
        int errors = HabitRules.validateCompletion(archived, today, today).fold(d -> 0, List::size);
        assertEquals(2, errors);
    }
}
