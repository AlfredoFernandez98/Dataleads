package dk.dataleads.service;

import dk.dataleads.dto.HealthResponse;
import org.springframework.stereotype.Service;

/**
 * Service-laget = forretningslogikken. Controlleren skal ikke "tænke" —
 * den modtager kald og delegerer hertil. Lige nu er logikken triviel,
 * men mønstret er på plads fra start: controller -> service -> (repository).
 *
 * @Service gør klassen til en Spring-bean, så den kan injectes i controlleren.
 */
@Service
public class HealthService {

    public HealthResponse check() {
        return new HealthResponse("UP", "dataleads");
    }
}
