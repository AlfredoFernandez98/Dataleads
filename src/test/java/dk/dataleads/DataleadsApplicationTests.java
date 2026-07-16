package dk.dataleads;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Fuld integrationstest: hele Spring-konteksten startes mod en rigtig
 * PostgreSQL i Docker (Testcontainers) — kræver ingen manuel database/.env.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DataleadsApplicationTests {

	@Test
	void contextLoads() {
	}

}
