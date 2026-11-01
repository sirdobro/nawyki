package cc.intmain.nawyki.http;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
public class FoundationRoutes {
    @Bean
    RouterFunction<ServerResponse> foundationApi(JdbcClient jdbc,
            @Value("${app.release:development}") String release) {
        return route()
                .GET("/api/health/live", request -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("status", "UP")))
                .GET("/api/health/ready", request -> {
                    try {
                        jdbc.sql("SELECT 1").query(Integer.class).single();
                        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of("status", "UP"));
                    } catch (RuntimeException unavailable) {
                        // Infrastructure errors are handled at the HTTP boundary.
                        return ServerResponse.status(503).contentType(MediaType.APPLICATION_JSON).body(Map.of("status", "DOWN"));
                    }
                })
                .GET("/api/status", request -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(Map.of(
                        "stage", "accounts-and-habits", "release", release,
                        "accountCreationEnabled", true)))
                .build();
    }
}
