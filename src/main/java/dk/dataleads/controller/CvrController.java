package dk.dataleads.controller;

import dk.dataleads.cvr.CvrClient;
import dk.dataleads.cvr.CvrCompany;
import dk.dataleads.cvr.CvrNumber;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rene CVR-opslag under /api/v1/cvr — INGEN persistering; til "slå firma op
 * før du opretter et lead"-flowet i UI'et. Selve importen sker via
 * POST /api/v1/leads/from-cvr/{cvr} (LeadController). Fejl (400/404/502)
 * renderes som ProblemDetail af GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/cvr")
public class CvrController {

    private final CvrClient cvrClient;

    public CvrController(CvrClient cvrClient) {
        this.cvrClient = cvrClient;
    }

    /** 200 med CvrCompany, 404 hvis CVR-nummeret ikke findes, 400 hvis ikke 8 cifre. */
    @GetMapping("/{cvr}")
    public CvrCompany lookup(@PathVariable String cvr) {
        CvrNumber.requireValid(cvr);
        return cvrClient.lookup(cvr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No company with CVR %s found in the CVR registry".formatted(cvr)));
    }
}
