package dk.dataleads;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers-opsætning: starter en rigtig PostgreSQL i Docker til tests.
 * @ServiceConnection fortæller Spring Boot at containeren ER datasourcen —
 * url/username/password bliver automatisk koblet på, ingen .env nødvendig.
 * Image matcher docker-compose.yml (postgres:16), så tests og dev er ens.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));
    }
}
