package cc.intmain.nawyki.http;

import cc.intmain.nawyki.application.AccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class WebSecurity {
    public static final String COOKIE = "__Host-nawyki-session";
    @Bean Clock applicationClock() { return Clock.systemUTC(); }

    @Bean SecurityFilterChain security(HttpSecurity http, AccountService accounts) throws Exception {
        var csrf = new CookieCsrfTokenRepository();
        csrf.setCookieName("__Host-nawyki-csrf");
        csrf.setCookieCustomizer(cookie -> cookie.secure(true).httpOnly(true).path("/").sameSite("Strict"));
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(config -> config.csrfTokenRepository(csrf))
                .formLogin(config -> config.disable()).httpBasic(config -> config.disable()).logout(config -> config.disable())
                .requestCache(config -> config.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/assets/**", "/favicon.ico", "/error", "/api/health/**", "/api/status",
                                "/api/auth/csrf", "/api/auth/me", "/api/auth/login", "/api/accounts").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) -> jsonError(response, 401, "UNAUTHENTICATED"))
                        .accessDeniedHandler((request, response, error) -> jsonError(response, 403, "CSRF_INVALID")))
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                            FilterChain chain) throws ServletException, IOException {
                        Optional<String> token = sessionToken(request);
                        token.flatMap(accounts::authenticate).ifPresent(id -> {
                            request.setAttribute("accountId", id);
                            var context = SecurityContextHolder.createEmptyContext();
                            context.setAuthentication(new UsernamePasswordAuthenticationToken(id.toString(), "", List.of()));
                            SecurityContextHolder.setContext(context);
                        });
                        // Exact Origin comparison supplements Spring's CSRF token validation.
                        String origin = request.getHeader("Origin");
                        boolean mutation = !List.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
                        if (mutation && origin != null && !origin.equals("https://nawyki.intmain.cc")) {
                            jsonError(response, 403, "ORIGIN_REJECTED");
                            return;
                        }
                        chain.doFilter(request, response);
                    }
                }, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    public static Optional<String> sessionToken(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies()).filter(c -> c.getName().equals(COOKIE)).map(c -> c.getValue()).findFirst();
    }
    private static void jsonError(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"code\":\"" + code + "\"}");
    }
}
