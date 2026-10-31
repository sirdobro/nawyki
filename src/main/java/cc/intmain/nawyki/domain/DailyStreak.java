package cc.intmain.nawyki.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Pure daily-habit calculation; scheduled and weekly streaks are a later step. */
public final class DailyStreak {
    private DailyStreak() {}

    public static int current(Collection<LocalDate> completions, LocalDate today) {
        Set<LocalDate> dates = completions.stream()
                .filter(date -> !date.isAfter(today))
                .collect(Collectors.toUnmodifiableSet());
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);
        int count = 0;
        while (dates.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }
}
