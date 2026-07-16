package dk.dataleads;

import org.springframework.boot.SpringApplication;

/**
 * Lokal udvikling uden manuel database: kør appen med en Testcontainers-
 * PostgreSQL via './mvnw spring-boot:test-run' (eller kør denne main i IDE'en).
 */
public class TestDataleadsApplication {

    public static void main(String[] args) {
        SpringApplication.from(DataleadsApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
