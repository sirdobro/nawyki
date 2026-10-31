package cc.intmain.nawyki.http;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FoundationApiIT {
    @Autowired TestRestTemplate http;
    @Autowired JdbcClient jdbc;
    @Autowired TransactionTemplate transaction;

    @Test void readinessChecksPostgresAndReturnsNoStore() {
        var response = http.getForEntity("/api/health/ready", String.class);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("UP"));
        assertEquals("no-store", response.getHeaders().getCacheControl());
    }

    @Test void statusDoesNotPretendAccountCreationExists() {
        var response = http.getForEntity("/api/status", String.class);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("\"accountCreationEnabled\":false"));
    }

    @Test void missingApiRouteIsNotReportedAsSuccess() {
        assertEquals(404, http.getForEntity("/api/not-a-route", String.class).getStatusCode().value());
    }

    @Test void migrationCreatesAnEmptyAccountSchemaAndTransactionsRollback() {
        UUID id = UUID.randomUUID();
        transaction.executeWithoutResult(status -> {
            jdbc.sql("INSERT INTO account (id) VALUES (:id)").param("id", id).update();
            status.setRollbackOnly();
        });
        assertEquals(0L, jdbc.sql("SELECT count(*) FROM account WHERE id=:id")
                .param("id", id).query(Long.class).single());
        assertEquals(1L, jdbc.sql("SELECT count(*) FROM flyway_schema_history WHERE success")
                .query(Long.class).single());
    }
}
