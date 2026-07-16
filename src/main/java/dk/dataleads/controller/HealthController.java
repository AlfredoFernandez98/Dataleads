package dk.dataleads.controller;

import dk.dataleads.dto.HealthResponse;
import dk.dataleads.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller-laget = det yderste lag, der håndterer HTTP.
 * @RestController betyder at returværdier automatisk bliver til JSON.
 * @RequestMapping("/api") giver alle endpoints i klassen præfikset /api.
 *
 * Vi injecter HealthService via constructoren (constructor injection) —
 * det er den anbefalede måde: nemt at teste og gør afhængigheder tydelige.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return healthService.check();
    }
}
