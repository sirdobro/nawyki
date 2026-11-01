package cc.intmain.nawyki.persistence;

import cc.intmain.nawyki.domain.HabitRules;
import cc.intmain.nawyki.domain.Schedule;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class HabitRepository {
    private final JdbcClient jdbc;
    public HabitRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<HabitRules.Habit> list(UUID account) {
        return jdbc.sql("SELECT * FROM habit WHERE account_id=:account ORDER BY created_at,id")
                .param("account", account).query(this::map).list();
    }
    public Optional<HabitRules.Habit> find(UUID account, UUID id) {
        return jdbc.sql("SELECT * FROM habit WHERE account_id=:account AND id=:id")
                .param("account", account).param("id", id).query(this::map).optional();
    }
    public HabitRules.Habit create(UUID account, HabitRules.Draft draft) {
        UUID id = UUID.randomUUID();
        String kind = type(draft.schedule());
        String days = draft.schedule() instanceof Schedule.SelectedDays selected
                ? selected.days().stream().map(day -> Integer.toString(day.getValue())).sorted().collect(Collectors.joining(",", "{", "}")) : "{}";
        int target = draft.schedule() instanceof Schedule.WeeklyGoal weekly ? weekly.frequency().days() : 0;
        jdbc.sql("""
            INSERT INTO habit(id,account_id,name,schedule_type,scheduled_days,weekly_target,start_date)
            VALUES (:id,:account,:name,:type,CAST(:days AS integer[]),:target,:start)
            """).param("id", id).param("account", account).param("name", draft.name()).param("type", kind)
                .param("days", days).param("target", target).param("start", draft.startDate()).update();
        return find(account, id).orElseThrow();
    }
    public void rename(UUID account, UUID id, String name) {
        jdbc.sql("UPDATE habit SET name=:name WHERE account_id=:account AND id=:id")
                .param("name", name).param("account", account).param("id", id).update();
    }
    public void archive(UUID account, UUID id, LocalDate date) {
        jdbc.sql("UPDATE habit SET archived_on=COALESCE(archived_on,:date) WHERE account_id=:account AND id=:id")
                .param("date", date).param("account", account).param("id", id).update();
    }
    public List<LocalDate> completions(UUID account, UUID id, LocalDate from, LocalDate to) {
        return jdbc.sql("SELECT completion_date FROM habit_completion WHERE account_id=:account AND habit_id=:id AND completion_date BETWEEN :from AND :to ORDER BY completion_date")
                .param("account", account).param("id", id).param("from", from).param("to", to).query(LocalDate.class).list();
    }
    public void complete(UUID account, UUID id, LocalDate date) {
        jdbc.sql("INSERT INTO habit_completion(account_id,habit_id,completion_date) VALUES (:account,:id,:date) ON CONFLICT(habit_id,completion_date) DO NOTHING")
                .param("account", account).param("id", id).param("date", date).update();
    }
    public void undo(UUID account, UUID id, LocalDate date) {
        jdbc.sql("DELETE FROM habit_completion WHERE account_id=:account AND habit_id=:id AND completion_date=:date")
                .param("account", account).param("id", id).param("date", date).update();
    }
    public void lock(UUID account, UUID id) {
        jdbc.sql("SELECT id FROM habit WHERE account_id=:account AND id=:id FOR UPDATE")
                .param("account", account).param("id", id).query(UUID.class).optional();
    }
    public static String type(Schedule schedule) {
        return switch (schedule) {
            case Schedule.Daily ignored -> "DAILY";
            case Schedule.SelectedDays ignored -> "SELECTED_DAYS";
            case Schedule.WeeklyGoal ignored -> "WEEKLY_GOAL";
        };
    }
    private HabitRules.Habit map(ResultSet row, int index) throws SQLException {
        Schedule schedule = switch (row.getString("schedule_type")) {
            case "SELECTED_DAYS" -> {
                Integer[] stored = (Integer[]) row.getArray("scheduled_days").getArray();
                var days = Arrays.stream(stored).map(DayOfWeek::of).collect(Collectors.toUnmodifiableSet());
                DayOfWeek first = days.iterator().next();
                yield new Schedule.SelectedDays(first, days.stream().filter(d -> d != first).collect(Collectors.toUnmodifiableSet()));
            }
            case "WEEKLY_GOAL" -> new Schedule.WeeklyGoal(Schedule.Frequency.values()[row.getInt("weekly_target") - 1]);
            default -> new Schedule.Daily();
        };
        return new HabitRules.Habit(row.getObject("id", UUID.class), row.getString("name"), schedule,
                row.getObject("start_date", LocalDate.class), Optional.ofNullable(row.getObject("archived_on", LocalDate.class)));
    }
}
