package cc.intmain.nawyki.http;

import cc.intmain.nawyki.application.AccountService;
import cc.intmain.nawyki.domain.WorkoutRules.*;
import cc.intmain.nawyki.persistence.WorkoutRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkoutApiIT {
    @Autowired TestRestTemplate http;
    @Autowired AccountService accounts;
    @Autowired WorkoutRepository repository;
    @Autowired JdbcClient jdbc;
    record Client(UUID owner, String cookies, String csrf) {}
    private final LocalDate today = LocalDate.now(ZoneId.of("Europe/Warsaw"));
    private Client client() {
        var created = accounts.create();
        var csrf = http.getForEntity("/api/auth/csrf", Map.class);
        String cookie = csrf.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream().filter(s -> s.startsWith("__Host-nawyki-csrf=")).findFirst().orElseThrow().split(";",2)[0];
        return new Client(created.login().accountId(), cookie + "; " + WebSecurity.COOKIE + "=" + created.login().sessionToken(), (String) csrf.getBody().get("token"));
    }
    private ResponseEntity<Map> call(Client c, HttpMethod method, String path, Object body) {
        var headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.COOKIE, c.cookies()); headers.set("X-XSRF-TOKEN", c.csrf());
        return http.exchange(path, method, new HttpEntity<>(body, headers), Map.class);
    }
    private Map<String,Object> draft(String name, int reps, String kg) {
        return Map.of("date", today.toString(), "exercises", List.of(Map.of("name", name, "sets", List.of(Map.of("repetitions", reps, "weightKg", kg), Map.of("repetitions", 6, "weightKg", "")))));
    }
    private String create(Client c, String name) {
        var response = call(c, HttpMethod.POST, "/api/workouts", draft(name,8,"40.125"));
        assertEquals(201, response.getStatusCode().value()); return response.getBody().get("id").toString();
    }
    @Test void savesReadsListsEditsExportsAndDeletesEntireWorkout() {
        var c = client(); String id = create(c,"Squat"); String path = "/api/workouts/"+id;
        var read = call(c,HttpMethod.GET,path,null); assertEquals(200,read.getStatusCode().value());
        assertTrue(read.getBody().toString().contains("40.125"));
        var exercises = (List<Map>) read.getBody().get("exercises");
        var sets = (List<Map>) exercises.getFirst().get("sets");
        assertEquals("",sets.get(1).get("weightKg"));
        var list = (List<Map>) call(c,HttpMethod.GET,"/api/workouts",null).getBody().get("workouts");
        assertEquals(2,list.getFirst().get("setCount")); assertEquals(List.of("Squat"),list.getFirst().get("exerciseNames"));
        assertEquals(200,call(c,HttpMethod.PUT,path,draft("Push-up",12,"")).getStatusCode().value());
        assertTrue(call(c,HttpMethod.GET,path,null).getBody().toString().contains("Push-up"));
        var export = call(c,HttpMethod.GET,"/api/account/export",null);
        assertEquals(2, export.getBody().get("formatVersion"));
        assertEquals(1, ((List<?>) export.getBody().get("workouts")).size());
        assertEquals(200,call(c,HttpMethod.DELETE,path,null).getStatusCode().value());
        assertEquals(404,call(c,HttpMethod.GET,path,null).getStatusCode().value());
        assertEquals(0L,jdbc.sql("SELECT count(*) FROM workout_exercise WHERE workout_id=CAST(:id AS uuid)").param("id",id).query(Long.class).single());
    }
    @Test void allWorkoutPathsAndExportAreAccountScopedAndRequireAuthAndCsrf() {
        var a=client(); var b=client(); String id=create(a,"Private workout"); String path="/api/workouts/"+id;
        assertEquals(List.of(),call(b,HttpMethod.GET,"/api/workouts",null).getBody().get("workouts"));
        assertEquals(404,call(b,HttpMethod.GET,path,null).getStatusCode().value());
        assertEquals(404,call(b,HttpMethod.PUT,path,draft("Stolen",8,"40")).getStatusCode().value());
        assertEquals(404,call(b,HttpMethod.DELETE,path,null).getStatusCode().value());
        assertEquals(List.of(),call(b,HttpMethod.GET,"/api/account/export",null).getBody().get("workouts"));
        assertEquals(401,http.getForEntity("/api/workouts",Map.class).getStatusCode().value());
        var headers=new HttpHeaders(); headers.set(HttpHeaders.COOKIE,a.cookies());
        assertEquals(403,http.exchange("/api/workouts",HttpMethod.POST,new HttpEntity<>(draft("Squat",8,"40"),headers),Map.class).getStatusCode().value());
        assertTrue(call(a,HttpMethod.GET,path,null).getBody().toString().contains("Private workout"));
    }
    @Test void invalidPayloadsAccumulateErrorsAndLeaveExistingWorkoutIntact() {
        var c=client(); String id=create(c,"Original");
        var invalid=Map.of("date",today.plusDays(1).toString(),"exercises",List.of(Map.of("name"," ","sets",List.of(Map.of("repetitions",0,"weightKg","-1")))));
        var response=call(c,HttpMethod.PUT,"/api/workouts/"+id,invalid);
        assertEquals(422,response.getStatusCode().value()); assertEquals(4,((List<?>)response.getBody().get("errors")).size());
        assertTrue(call(c,HttpMethod.GET,"/api/workouts/"+id,null).getBody().toString().contains("Original"));
        assertEquals(400,call(c,HttpMethod.POST,"/api/workouts",Map.of("exercises",true)).getStatusCode().value());
        assertEquals(400,call(c,HttpMethod.POST,"/api/workouts","{broken").getStatusCode().value());
        assertEquals(400,call(c,HttpMethod.POST,"/api/workouts",Map.of("padding","x".repeat(17000))).getStatusCode().value());
        assertEquals(422,call(c,HttpMethod.GET,"/api/workouts?from="+today+"&to="+today.minusDays(1),null).getStatusCode().value());
        assertEquals(List.of(),call(c,HttpMethod.GET,"/api/workouts?from="+today.minusDays(2)+"&to="+today.minusDays(1),null).getBody().get("workouts"));
    }
    @Test void databaseFailureRollsBackPartialCreateAndFullReplacement() {
        var c=client(); String id=create(c,"Original");
        var invalid = new Workout(today.minusDays(1), List.of(
            new Exercise("Inserted first",List.of(new SetEntry(8,Optional.empty()))),
            new Exercise("Fails second",List.of(new SetEntry(0,Optional.empty())))));
        assertThrows(DataIntegrityViolationException.class,()->repository.create(c.owner(),invalid));
        assertEquals(1L,jdbc.sql("SELECT count(*) FROM workout WHERE account_id=:account").param("account",c.owner()).query(Long.class).single());
        assertThrows(DataIntegrityViolationException.class,()->repository.replace(c.owner(),UUID.fromString(id),invalid));
        var original=repository.find(c.owner(),UUID.fromString(id)).orElseThrow().workout();
        assertEquals(today,original.date()); assertEquals("Original",original.exercises().getFirst().name()); assertEquals(2,original.exercises().getFirst().sets().size());
    }
    @Test void concurrentReplacementsDoNotMixExerciseSets() throws Exception {
        var c=client(); String id=create(c,"Original");
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks=List.<Callable<Integer>>of(
                ()->call(c,HttpMethod.PUT,"/api/workouts/"+id,draft("First",3,"10")).getStatusCode().value(),
                ()->call(c,HttpMethod.PUT,"/api/workouts/"+id,draft("Second",9,"20")).getStatusCode().value());
            for(var result:executor.invokeAll(tasks)) assertEquals(200,result.get());
        }
        var exercise=repository.find(c.owner(),UUID.fromString(id)).orElseThrow().workout().exercises().getFirst();
        assertEquals(2,exercise.sets().size());
        if(exercise.name().equals("First")) assertEquals(3,exercise.sets().getFirst().repetitions());
        else { assertEquals("Second",exercise.name()); assertEquals(9,exercise.sets().getFirst().repetitions()); }
    }
    @Test void deletingAccountCascadesWorkoutAndDoesNotCompleteHabits() {
        var c=client();
        assertEquals(201,call(c,HttpMethod.POST,"/api/habits",Map.of("name","Idź na trening","scheduleType","DAILY","startDate",today.toString())).getStatusCode().value());
        String id=create(c,"Squat");
        assertEquals(0L,jdbc.sql("SELECT count(*) FROM habit_completion WHERE account_id=:id").param("id",c.owner()).query(Long.class).single());
        jdbc.sql("DELETE FROM account WHERE id=:id").param("id",c.owner()).update();
        assertEquals(0L,jdbc.sql("SELECT count(*) FROM workout WHERE id=CAST(:id AS uuid)").param("id",id).query(Long.class).single());
    }
    @Test void multipleExercisesAndSetsRoundTripInTheirOriginalOrder() {
        var c=client();
        var payload=Map.of("date",today.toString(),"exercises",List.of(
            Map.of("name","Squat","sets",List.of(Map.of("repetitions",8,"weightKg","40,125"),Map.of("repetitions",6,"weightKg","42.5"))),
            Map.of("name","Push-up","sets",List.of(Map.of("repetitions",12,"weightKg",""),Map.of("repetitions",10,"weightKg","0")))));
        var created=call(c,HttpMethod.POST,"/api/workouts",payload);
        assertEquals(201,created.getStatusCode().value());
        var exercises=(List<Map>)call(c,HttpMethod.GET,"/api/workouts/"+created.getBody().get("id"),null).getBody().get("exercises");
        assertEquals(List.of("Squat","Push-up"),exercises.stream().map(e->e.get("name")).toList());
        var first=(List<Map>)exercises.getFirst().get("sets");
        assertEquals(List.of(8,6),first.stream().map(s->s.get("repetitions")).toList());
        assertEquals("40.125",first.getFirst().get("weightKg"));
        var second=(List<Map>)exercises.get(1).get("sets");
        assertEquals("",second.getFirst().get("weightKg"));
        assertEquals("0",second.get(1).get("weightKg"));
    }
}
