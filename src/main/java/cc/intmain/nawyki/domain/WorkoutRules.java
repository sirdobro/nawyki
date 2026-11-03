package cc.intmain.nawyki.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static cc.intmain.nawyki.domain.Result.*;

public final class WorkoutRules {
    private WorkoutRules() {}
    public record SetInput(String repetitions, String weightKg) {}
    public record ExerciseInput(String name, List<SetInput> sets) {
        public ExerciseInput { sets = List.copyOf(sets); }
    }
    public record SetEntry(int repetitions, Optional<BigDecimal> weightKg) {}
    public record Exercise(String name, List<SetEntry> sets) {
        public Exercise { sets = List.copyOf(sets); }
    }
    public record Workout(LocalDate date, List<Exercise> exercises) {
        public Workout { exercises = List.copyOf(exercises); }
    }

    public static Result<Workout> validate(Optional<LocalDate> date, List<ExerciseInput> input, LocalDate today) {
        var errors = new ArrayList<Violation>();
        if (date.isEmpty() || date.orElse(today).isAfter(today))
            errors.add(new Violation("date", "Podaj datę treningu nie późniejszą niż dzisiaj."));
        if (input.isEmpty() || input.size() > 30)
            errors.add(new Violation("exercises", "Trening musi zawierać od 1 do 30 ćwiczeń."));
        var exercises = new ArrayList<Exercise>();
        for (int i = 0; i < Math.min(input.size(), 30); i++) {
            var source = input.get(i);
            String name = source.name().strip();
            String field = "exercises[" + i + "]";
            if (name.isEmpty() || name.length() > 100)
                errors.add(new Violation(field + ".name", "Nazwa ćwiczenia musi mieć od 1 do 100 znaków."));
            if (source.sets().isEmpty() || source.sets().size() > 30)
                errors.add(new Violation(field + ".sets", "Ćwiczenie musi zawierać od 1 do 30 serii."));
            var sets = new ArrayList<SetEntry>();
            for (int j = 0; j < Math.min(source.sets().size(), 30); j++) {
                var raw = source.sets().get(j);
                String reps = raw.repetitions().strip();
                String weight = raw.weightKg().strip().replace(',', '.');
                boolean validReps = reps.matches("[0-9]{1,5}") && Integer.parseInt(reps) >= 1 && Integer.parseInt(reps) <= 10000;
                boolean validWeight = weight.isEmpty() || weight.matches("[0-9]{1,4}(\\.[0-9]{1,3})?");
                if (!validReps) errors.add(new Violation(field + ".sets[" + j + "].repetitions", "Podaj całkowitą liczbę powtórzeń od 1 do 10000."));
                if (!validWeight) errors.add(new Violation(field + ".sets[" + j + "].weightKg", "Ciężar musi wynosić od 0 do 9999,999 kg, z maksymalnie trzema miejscami po przecinku."));
                if (validReps && validWeight) sets.add(new SetEntry(Integer.parseInt(reps), weight.isEmpty() ? Optional.empty() : Optional.of(new BigDecimal(weight))));
            }
            exercises.add(new Exercise(name, sets));
        }
        return errors.isEmpty() ? new Ok<>(new Workout(date.orElse(today), exercises)) : new Invalid<>(errors);
    }
}
