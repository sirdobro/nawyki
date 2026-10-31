package cc.intmain.nawyki.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.integers;

class DailyStreakTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    @Test void emptyHistoryHasNoStreak() {
        assertEquals(0, DailyStreak.current(List.of(), TODAY));
    }

    @Test void unfinishedTodayPreservesYesterdayStreak() {
        assertEquals(2, DailyStreak.current(List.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY));
    }

    @Test void missedClosedDayBreaksTheStreak() {
        assertEquals(0, DailyStreak.current(List.of(TODAY.minusDays(2)), TODAY));
    }

    @Test void completingTodayExtendsTheStreak() {
        assertEquals(3, DailyStreak.current(List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY));
    }

    @Test void duplicateDatesDoNotInflateTheStreak() {
        qt().withFixedSeed(101).forAll(integers().between(1, 180)).check(length -> {
            var dates = consecutiveDays(length);
            var duplicated = new ArrayList<>(dates);
            duplicated.addAll(dates);
            return DailyStreak.current(duplicated, TODAY) == length;
        });
    }

    @Test void historyOrderDoesNotChangeTheStreak() {
        qt().withFixedSeed(102).forAll(integers().between(1, 180)).check(length -> {
            var original = consecutiveDays(length);
            var reversed = new ArrayList<>(original);
            Collections.reverse(reversed);
            return DailyStreak.current(original, TODAY) == DailyStreak.current(reversed, TODAY);
        });
    }

    @Test void futureRecordsDoNotChangeTheCurrentStreak() {
        qt().withFixedSeed(103).forAll(integers().between(0, 180), integers().between(1, 90))
                .check((length, futureOffset) -> {
                    var dates = new ArrayList<>(consecutiveDays(length));
                    dates.add(TODAY.plusDays(futureOffset));
                    return DailyStreak.current(dates, TODAY) == length;
                });
    }

    private static List<LocalDate> consecutiveDays(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(TODAY::minusDays).toList();
    }
}
