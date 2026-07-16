package dk.dataleads.service;

import dk.dataleads.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.stereotype.Service;

/**
 * Service-laget = forretningslogikken. Controlleren skal ikke "tænke" —
 * den modtager kald og delegerer hertil.
 *
 * Status er IKKE hardcodet: vi spørger Spring Boot Actuator's HealthEndpoint,
 * som aggregerer alle health-indikatorer (fx databasen). Svarer den DOWN,
 * siger vores /api/v1/health det også. Actuator's eget /actuator/health er
 * fortsat den kanoniske probe for deployment-platformen.
 */
@Service
public class HealthService {

    private final HealthEndpoint healthEndpoint;
    private final String appName;

    public HealthService(HealthEndpoint healthEndpoint,
                         @Value("${spring.application.name:dataleads}") String appName) {
        this.healthEndpoint = healthEndpoint;
        this.appName = appName;
    }

    public HealthResponse check() {
        String status = healthEndpoint.health().getStatus().getCode();
        return new HealthResponse(status, appName);
    }
}
