package cc.intmain.nawyki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
public class NawykiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NawykiApplication.class, args);
    }
}
