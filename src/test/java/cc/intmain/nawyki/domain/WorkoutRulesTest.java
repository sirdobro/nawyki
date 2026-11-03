package cc.intmain.nawyki.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static cc.intmain.nawyki.domain.WorkoutRules.*;

class WorkoutRulesTest {
    private final LocalDate today = LocalDate.of(2026, 11, 3);
    private Result<Workout> validate(List<ExerciseInput> exercises) { return WorkoutRules.validate(Optional.of(today), exercises, today); }
    private ExerciseInput exercise(String name, String reps, String kg) { return new ExerciseInput(name, List.of(new SetInput(reps, kg))); }
    @Test void preservesOrderOptionalWeightAndExactDecimalIncludingComma() {
        var result = validate(List.of(new ExerciseInput(" Squat ", List.of(new SetInput("8", "40,125"), new SetInput("6", ""), new SetInput("5", "0"))), exercise("Push-up", "12", "")));
        var workout = ((Result.Ok<Workout>) result).value();
        assertEquals(List.of("Squat", "Push-up"), workout.exercises().stream().map(Exercise::name).toList());
        assertEquals(List.of(8, 6, 5), workout.exercises().getFirst().sets().stream().map(SetEntry::repetitions).toList());
        assertEquals(Optional.of(new BigDecimal("40.125")), workout.exercises().getFirst().sets().getFirst().weightKg());
        assertTrue(workout.exercises().getFirst().sets().get(1).weightKg().isEmpty());
        assertEquals(Optional.of(BigDecimal.ZERO), workout.exercises().getFirst().sets().get(2).weightKg());
    }
    @Test void accumulatesDateNameAndSetErrorsWithoutThrowing() {
        var invalid = WorkoutRules.validate(Optional.empty(), List.of(exercise(" ", "0", "-1")), today);
        int count = invalid.fold(w -> 0, List::size);
        assertEquals(4, count);
        assertInstanceOf(Result.Invalid.class, WorkoutRules.validate(Optional.of(today.plusDays(1)), List.of(exercise("Squat", "8", "40")), today));
    }
    @Test void rejectsEmptyAndOversizedCollectionsAndNames() {
        assertInstanceOf(Result.Invalid.class, validate(List.of()));
        assertInstanceOf(Result.Invalid.class, validate(Collections.nCopies(31, exercise("Squat", "8", ""))));
        assertInstanceOf(Result.Invalid.class, validate(List.of(new ExerciseInput("Squat", List.of()))));
        assertInstanceOf(Result.Invalid.class, validate(List.of(new ExerciseInput("Squat", Collections.nCopies(31, new SetInput("8", ""))))));
        assertInstanceOf(Result.Invalid.class, validate(List.of(exercise("x".repeat(101), "8", ""))));
    }
    @Test void rejectsInvalidNumbersAndAcceptsBounds() {
        for (String reps : List.of("", "0", "-1", "1.5", "10001", "999999999999999999", "oops"))
            assertInstanceOf(Result.Invalid.class, validate(List.of(exercise("Squat", reps, "40"))));
        for (String weight : List.of("-1", "10000", "1.0001", "NaN", "1e3", ".5"))
            assertInstanceOf(Result.Invalid.class, validate(List.of(exercise("Squat", "8", weight))));
        assertInstanceOf(Result.Ok.class, validate(List.of(exercise("Squat", "10000", "9999.999"))));
        assertInstanceOf(Result.Ok.class, validate(List.of(exercise("Squat", "1", " 0,001 "))));
    }
    @Test void copiesEveryCollectionBoundary() {
        var inputs = new ArrayList<>(List.of(new SetInput("8", "40")));
        var raw = new ExerciseInput("Squat", inputs); inputs.clear(); assertEquals(1, raw.sets().size());
        var sets = new ArrayList<>(List.of(new SetEntry(8, Optional.empty())));
        var exercise = new Exercise("Squat", sets); sets.clear();
        var exercises = new ArrayList<>(List.of(exercise));
        var workout = new Workout(today, exercises); exercises.clear();
        assertEquals(1, workout.exercises().getFirst().sets().size());
        assertThrows(UnsupportedOperationException.class, () -> workout.exercises().clear());
        assertThrows(UnsupportedOperationException.class, () -> exercise.sets().clear());
        assertThrows(UnsupportedOperationException.class, () -> raw.sets().clear());
    }
}
