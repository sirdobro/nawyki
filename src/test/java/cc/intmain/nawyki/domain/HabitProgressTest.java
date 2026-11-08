package cc.intmain.nawyki.domain;

import java.time.*;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.integers;

class HabitProgressTest {
    private static final LocalDate START = LocalDate.of(2026, 10, 7); // Wednesday
    private HabitRules.Habit habit(Schedule schedule, Optional<LocalDate> archive) {
        return new HabitRules.Habit(UUID.randomUUID(), "Czytać", schedule, START, archive);
    }
    private HabitProgress.Progress progress(HabitRules.Habit h, List<LocalDate> dates, LocalDate today) {
        return HabitProgress.calculate(h, dates, START, today, today);
    }
    private Schedule weekly() { return new Schedule.WeeklyGoal(Schedule.Frequency.THREE); }

    @Test void selectedDaysSkipFreeDaysAndLeaveTodayOpen() {
        var h = habit(new Schedule.SelectedDays(DayOfWeek.WEDNESDAY, Set.of(DayOfWeek.FRIDAY)), Optional.empty());
        var p = progress(h, List.of(START, START.plusDays(2)), START.plusDays(7));
        assertEquals(2, p.streak().current()); assertEquals(2, p.streak().best());
        assertEquals(3, p.scheduled()); assertEquals(HabitProgress.Status.IN_PROGRESS, p.periods().get(2).status());
        assertEquals(0, progress(h, List.of(START, START.plusDays(2)), START.plusDays(8)).streak().current());
    }
    @Test void dailyBestSurvivesMissedClosedDayAndTodayExtendsCurrent() {
        var h = habit(new Schedule.Daily(), Optional.empty());
        var p = progress(h, List.of(START, START.plusDays(1), START.plusDays(3)), START.plusDays(3));
        assertEquals(1, p.streak().current()); assertEquals(2, p.streak().best());
    }
    @Test void weeklyPeriodsAreWednesdayToTuesdayAndOpenWeekPreservesStreak() {
        var h = habit(weekly(), Optional.empty());
        var dates = List.of(START, START.plusDays(1), START.plusDays(6));
        var p = progress(h, dates, START.plusDays(8));
        assertEquals(1, p.streak().current()); assertEquals("WEEKS", p.streak().unit());
        assertEquals(START, p.periods().getFirst().from());
        assertEquals(START.plusDays(6), p.periods().getFirst().to());
        assertEquals(HabitProgress.Status.IN_PROGRESS, p.periods().getLast().status());
        assertEquals(0, progress(h, dates, START.plusDays(14)).streak().current());
    }
    @Test void fulfilledCurrentWeekExtendsStreakAndExtraDaysStayVisible() {
        var h = habit(weekly(), Optional.empty());
        var dates = IntStream.of(0, 1, 2, 7, 8, 9, 10).mapToObj(START::plusDays).toList();
        var p = progress(h, dates, START.plusDays(10));
        assertEquals(2, p.streak().current()); assertEquals(2, p.streak().best());
        assertEquals(4, p.periods().getLast().completed()); assertEquals(3, p.periods().getLast().target());
    }
    @Test void archiveFreezesDailyStreakAndRejectsRawOutOfRangeHistory() {
        var h = habit(new Schedule.Daily(), Optional.of(START.plusDays(2)));
        var p = progress(h, List.of(START.minusDays(1), START, START.plusDays(1), START.plusDays(2), START.plusDays(30)), START.plusDays(20));
        assertEquals(2, p.completed()); assertEquals(2, p.streak().current()); assertTrue(p.streak().archived());
    }
    @Test void truncatedArchivedWeekIsSkippedUnlessTargetWasMet() {
        var h = habit(weekly(), Optional.of(START.plusDays(9)));
        var dates = List.of(START, START.plusDays(1), START.plusDays(2), START.plusDays(7));
        var p = progress(h, dates, START.plusDays(20));
        assertEquals(1, p.streak().current()); assertEquals(HabitProgress.Status.PARTIAL, p.periods().getLast().status());
        var met = habit(weekly(), Optional.of(START.plusDays(10)));
        assertEquals(2, progress(met, IntStream.of(0,1,2,7,8,9).mapToObj(START::plusDays).toList(), START.plusDays(20)).streak().current());
    }
    @Test void archiveAtWeekBoundaryDoesNotForgiveFailedFullWeek() {
        var h = habit(weekly(), Optional.of(START.plusDays(14)));
        var p = progress(h, List.of(START, START.plusDays(1), START.plusDays(2)), START.plusDays(20));
        assertEquals(HabitProgress.Status.MISSED, p.periods().getLast().status());
        assertEquals(0, p.streak().current()); assertEquals(1, p.streak().best());
    }
    @Test void reportingRangeDoesNotTruncateStreakOrWeeklyCounts() {
        var h = habit(weekly(), Optional.empty());
        var p = HabitProgress.calculate(h, List.of(START, START.plusDays(1), START.plusDays(2)), START.plusDays(2), START.plusDays(2), START.plusDays(2));
        assertEquals(1, p.completed()); assertEquals(3, p.periods().getFirst().completed());
        assertEquals(1, p.streak().current());
        assertThrows(UnsupportedOperationException.class, () -> p.periods().clear());
    }
    @Test void emptyAndNotYetStartedHabitsHaveNoStreak() {
        var h = habit(new Schedule.Daily(), Optional.empty());
        assertEquals(0, progress(h, List.of(), START.minusDays(1)).streak().best());
        assertEquals(List.of(), progress(h, List.of(), START.minusDays(1)).periods());
        assertEquals(0, progress(habit(new Schedule.Daily(), Optional.of(START)), List.of(START), START).scheduled());
    }
    @Test void orderDuplicatesAndFutureDatesDoNotChangeAnyScheduleResult() {
        qt().withFixedSeed(211).forAll(integers().between(0, 80)).check(length -> {
            var today = START.plusDays(80);
            var dates = IntStream.range(0, length).mapToObj(START::plusDays).toList();
            var noise = new ArrayList<>(dates); noise.addAll(dates); noise.add(today.plusDays(1)); Collections.reverse(noise);
            return List.of(new Schedule.Daily(), weekly(), new Schedule.SelectedDays(DayOfWeek.WEDNESDAY, Set.of(DayOfWeek.FRIDAY)))
                    .stream().allMatch(s -> progress(habit(s, Optional.empty()), dates, today)
                            .equals(progress(habit(s, Optional.empty()), noise, today)));
        });
    }
    @Test void unscheduledRawDatesNeverChangeSelectedDayProgress() {
        qt().withFixedSeed(212).forAll(integers().between(1, 60)).check(weeks -> {
            var h = habit(new Schedule.SelectedDays(DayOfWeek.WEDNESDAY, Set.of()), Optional.empty());
            var scheduled = IntStream.range(0, weeks).mapToObj(START::plusWeeks).toList();
            var noisy = new ArrayList<>(scheduled); scheduled.forEach(d -> noisy.add(d.plusDays(1)));
            return progress(h, scheduled, START.plusWeeks(weeks)).equals(progress(h, noisy, START.plusWeeks(weeks)));
        });
    }
}
