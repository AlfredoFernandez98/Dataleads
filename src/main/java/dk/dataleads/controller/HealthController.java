package dk.dataleads.controller;

import dk.dataleads.dto.HealthResponse;
import dk.dataleads.service.HealthService;
import org.springframework.boot.health.contributor.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller-laget = det yderste lag, der håndterer HTTP.
 * @RestController betyder at returværdier automatisk bliver til JSON.
 * @RequestMapping("/api/v1") giver alle endpoints i klassen præfikset /api/v1
 * — API'et er versioneret fra start, så breaking changes kan blive /api/v2.
 *
 * Vi injecter HealthService via constructoren (constructor injection) —
 * det er den anbefalede måde: nemt at teste og gør afhængigheder tydelige.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = healthService.check();
        boolean up = Status.UP.getCode().equals(response.status());
        return ResponseEntity
                .status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
