package dk.dataleads.controller;

import dk.dataleads.cvr.CvrNumber;
import dk.dataleads.domain.Lead;
import dk.dataleads.domain.LeadStatus;
import dk.dataleads.dto.CreateLeadRequest;
import dk.dataleads.dto.LeadResponse;
import dk.dataleads.dto.UpdateLeadStatusRequest;
import dk.dataleads.service.LeadService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST-API for leads under /api/v1/leads. Controlleren er bevidst tynd:
 * validering via @Valid, delegation til LeadService, og mapping
 * entity -> LeadResponse via den statiske fabrik. Fejl (404/409/400)
 * renderes som ProblemDetail af GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    /** 201 Created med Location-header pegende på det nye lead. */
    @PostMapping
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody CreateLeadRequest request) {
        Lead lead = leadService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(lead.getId())
                .toUri();
        return ResponseEntity.created(location).body(LeadResponse.from(lead));
    }

    /**
     * Importerer et lead direkte fra CVR-registret (ADR-0002): 201 + Location,
     * 400 ved ugyldigt CVR-format, 404 ved ukendt CVR, 409 ved dublet.
     */
    @PostMapping("/from-cvr/{cvr}")
    public ResponseEntity<LeadResponse> importFromCvr(@PathVariable String cvr) {
        CvrNumber.requireValid(cvr);
        LeadResponse response = leadService.importFromCvr(cvr);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/leads/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /** Genopfrisker registerfelterne fra CVR (30-dages friskhedspolitik, ADR-0002). */
    @PostMapping("/{id}/refresh-cvr")
    public LeadResponse refreshFromCvr(@PathVariable Long id) {
        return leadService.refreshFromCvr(id);
    }

    /** Pagineret liste (?page=&size=&sort=), valgfrit filtreret på ?status=. */
    @GetMapping
    public Page<LeadResponse> list(@RequestParam(required = false) LeadStatus status, Pageable pageable) {
        return leadService.list(status, pageable).map(LeadResponse::from);
    }

    @GetMapping("/{id}")
    public LeadResponse get(@PathVariable Long id) {
        return LeadResponse.from(leadService.get(id));
    }

    @PatchMapping("/{id}/status")
    public LeadResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateLeadStatusRequest request) {
        return LeadResponse.from(leadService.updateStatus(id, request));
    }

    /** Soft delete — 204 No Content; leadet forsvinder fra API'et med det samme. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        leadService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
