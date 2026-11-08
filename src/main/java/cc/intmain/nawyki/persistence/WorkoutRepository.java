package cc.intmain.nawyki.persistence;

import cc.intmain.nawyki.domain.WorkoutRules.*;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class WorkoutRepository {
    public record Stored(UUID id, Workout workout) {}
    public record Summary(UUID id, LocalDate date, List<String> exerciseNames, int setCount) {
        public Summary { exerciseNames = List.copyOf(exerciseNames); }
    }
    private record Header(UUID id, LocalDate date) {}
    private final JdbcClient jdbc;
    private final TransactionTemplate transaction;
    public WorkoutRepository(JdbcClient jdbc, TransactionTemplate transaction) {
        this.jdbc = jdbc; this.transaction = transaction;
    }
    public int count(UUID account, LocalDate from, LocalDate to) {
        return jdbc.sql("SELECT count(*) FROM workout WHERE account_id=:account AND workout_date BETWEEN :from AND :to")
                .param("account", account).param("from", from).param("to", to).query(Integer.class).single();
    }
    public List<Summary> list(UUID account, LocalDate from, LocalDate to) {
        return jdbc.sql("""
            SELECT w.id,w.workout_date,
            ARRAY(SELECT e.name FROM workout_exercise e WHERE e.workout_id=w.id ORDER BY e.position) AS names,
            (SELECT count(*) FROM workout_set s JOIN workout_exercise e ON e.id=s.exercise_id WHERE e.workout_id=w.id) AS set_count
            FROM workout w WHERE w.account_id=:account AND w.workout_date BETWEEN :from AND :to
            ORDER BY w.workout_date DESC,w.created_at DESC,w.id
            """).param("account", account).param("from", from).param("to", to)
                .query((row, n) -> new Summary(row.getObject("id", UUID.class), row.getObject("workout_date", LocalDate.class),
                        Arrays.asList((String[]) row.getArray("names").getArray()), row.getInt("set_count"))).list();
    }
    public Optional<Stored> find(UUID account, UUID id) {
        return transaction.execute(status -> jdbc.sql("SELECT workout_date FROM workout WHERE account_id=:account AND id=:id FOR SHARE")
                .param("account", account).param("id", id).query(LocalDate.class).optional().map(date -> load(id, date)));
    }
    public List<Stored> export(UUID account) {
        return transaction.execute(status -> jdbc.sql("SELECT id,workout_date FROM workout WHERE account_id=:account ORDER BY workout_date,id FOR SHARE")
                .param("account", account).query((row, n) -> new Header(row.getObject("id", UUID.class), row.getObject("workout_date", LocalDate.class)))
                .list().stream().map(header -> load(header.id(), header.date())).toList());
    }
    public Stored create(UUID account, Workout workout) {
        return transaction.execute(status -> {
            UUID id = UUID.randomUUID();
            jdbc.sql("INSERT INTO workout(id,account_id,workout_date) VALUES (:id,:account,:date)")
                    .param("id", id).param("account", account).param("date", workout.date()).update();
            writeExercises(id, workout.exercises());
            return new Stored(id, workout);
        });
    }
    public boolean replace(UUID account, UUID id, Workout workout) {
        return transaction.execute(status -> {
            if (!lock(account, id)) return false;
            jdbc.sql("UPDATE workout SET workout_date=:date WHERE account_id=:account AND id=:id")
                    .param("date", workout.date()).param("account", account).param("id", id).update();
            jdbc.sql("DELETE FROM workout_exercise WHERE workout_id=:id").param("id", id).update();
            writeExercises(id, workout.exercises());
            return true;
        });
    }
    public boolean delete(UUID account, UUID id) {
        return jdbc.sql("DELETE FROM workout WHERE account_id=:account AND id=:id")
                .param("account", account).param("id", id).update() == 1;
    }
    private boolean lock(UUID account, UUID id) {
        return jdbc.sql("SELECT id FROM workout WHERE account_id=:account AND id=:id FOR UPDATE")
                .param("account", account).param("id", id).query(UUID.class).optional().isPresent();
    }
    private Stored load(UUID id, LocalDate date) {
        var exercises = jdbc.sql("SELECT id,name FROM workout_exercise WHERE workout_id=:id ORDER BY position")
                .param("id", id).query((row, n) -> {
                    UUID exercise = row.getObject("id", UUID.class);
                    var sets = jdbc.sql("SELECT repetitions,weight_kg FROM workout_set WHERE exercise_id=:id ORDER BY position")
                            .param("id", exercise).query((set, index) -> new SetEntry(set.getInt("repetitions"),
                                    Optional.ofNullable(set.getBigDecimal("weight_kg")))).list();
                    return new Exercise(row.getString("name"), sets);
                }).list();
        return new Stored(id, new Workout(date, exercises));
    }
    private void writeExercises(UUID workout, List<Exercise> exercises) {
        for (int i = 0; i < exercises.size(); i++) {
            var exercise = exercises.get(i); UUID id = UUID.randomUUID();
            jdbc.sql("INSERT INTO workout_exercise(id,workout_id,position,name) VALUES (:id,:workout,:position,:name)")
                    .param("id", id).param("workout", workout).param("position", i).param("name", exercise.name()).update();
            for (int j = 0; j < exercise.sets().size(); j++) {
                var set = exercise.sets().get(j);
                jdbc.sql("INSERT INTO workout_set(exercise_id,position,repetitions,weight_kg) VALUES (:id,:position,:reps,:weight)")
                        .param("id", id).param("position", j).param("reps", set.repetitions())
                        .param("weight", set.weightKg().orElse(null), Types.NUMERIC).update();
            }
        }
    }
}
