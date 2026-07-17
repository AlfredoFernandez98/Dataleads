package dk.dataleads.service;

import dk.dataleads.cvr.CvrClient;
import dk.dataleads.cvr.CvrCompany;
import dk.dataleads.domain.Lead;
import dk.dataleads.domain.LeadActivity;
import dk.dataleads.domain.LeadStatus;
import dk.dataleads.dto.CreateLeadRequest;
import dk.dataleads.dto.LeadResponse;
import dk.dataleads.dto.UpdateLeadStatusRequest;
import dk.dataleads.repository.LeadActivityRepository;
import dk.dataleads.repository.LeadRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Forretningslogik for leads. Fejl kastes som ResponseStatusException
 * (404/409), så GlobalExceptionHandler renderer dem som ProblemDetail —
 * servicen kender status-koderne, controlleren forbliver "dum".
 * Læsning er @Transactional(readOnly = true) på klassen; skrivende metoder
 * overstyrer med en almindelig @Transactional.
 */
@Service
@Transactional(readOnly = true)
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final CvrClient cvrClient;

    public LeadService(LeadRepository leadRepository, LeadActivityRepository leadActivityRepository,
                       CvrClient cvrClient) {
        this.leadRepository = leadRepository;
        this.leadActivityRepository = leadActivityRepository;
        this.cvrClient = cvrClient;
    }

    /**
     * Opretter et lead. Dubleret CVR afvises som 409 — CVR er det naturlige
     * unikke nøglefelt (UNIQUE(cvr) i skemaet er sidste forsvarslinje).
     */
    @Transactional
    public Lead create(CreateLeadRequest request) {
        if (leadRepository.existsByCvrAndDeletedAtIsNull(request.cvr())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A lead with CVR %s already exists".formatted(request.cvr()));
        }
        Lead lead = new Lead(request.cvr(), request.name(), request.address(),
                request.industryCode(), request.reklamebeskyttet());
        return leadRepository.save(lead);
    }

    /** Alle ikke-slettede leads, pagineret; valgfrit filtreret på status. */
    public Page<Lead> list(LeadStatus status, Pageable pageable) {
        if (status != null) {
            return leadRepository.findAllByStatusAndDeletedAtIsNull(status, pageable);
        }
        return leadRepository.findAllByDeletedAtIsNull(pageable);
    }

    /** Ét lead — soft-deleted rækker opfører sig som om de ikke findes (404). */
    public Lead get(Long id) {
        return leadRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lead %d not found".formatted(id)));
    }

    /**
     * Skifter status og dokumenterer skiftet som en STATUS_CHANGE-aktivitet
     * (gammel -> ny status + valgfri note) i samme transaktion.
     */
    @Transactional
    public Lead updateStatus(Long id, UpdateLeadStatusRequest request) {
        Lead lead = get(id);
        LeadStatus oldStatus = lead.getStatus();
        lead.setStatus(request.status());
        Lead saved = leadRepository.save(lead);
        leadActivityRepository.save(
                LeadActivity.statusChange(saved, oldStatus, request.status(), request.note()));
        return saved;
    }

    /**
     * Opretter et lead direkte fra et CVR-opslag (ADR-0002). Firmadata
     * (navn/adresse/branche/reklamebeskyttelse) kommer fra registret, og
     * cvr_synced_at sættes til nu, så 30-dages-friskhedspolitikken kan
     * håndhæves. Dublet -> 409, ukendt CVR -> 404 (kilde-nedbrud bliver
     * 502 inde i CvrClient).
     */
    @Transactional
    public LeadResponse importFromCvr(String cvr) {
        if (leadRepository.existsByCvrAndDeletedAtIsNull(cvr)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A lead with CVR %s already exists".formatted(cvr));
        }
        CvrCompany company = cvrClient.lookup(cvr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No company with CVR %s found in the CVR registry".formatted(cvr)));
        Lead lead = new Lead(cvr, company.name(), company.fullAddress(),
                company.industryCode(), company.reklamebeskyttet());
        lead.setCvrSyncedAt(Instant.now());
        return LeadResponse.from(leadRepository.save(lead));
    }

    /**
     * Genopfrisker et eksisterende leads firmadata fra CVR (30-dages
     * refresh-politikken, ADR-0002). Leadets status/aktiviteter røres ikke —
     * kun registerfelterne og cvr_synced_at.
     */
    @Transactional
    public LeadResponse refreshFromCvr(Long id) {
        Lead lead = get(id);
        CvrCompany company = cvrClient.lookup(lead.getCvr())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "CVR %s no longer exists in the CVR registry".formatted(lead.getCvr())));
        lead.setName(company.name());
        lead.setAddress(company.fullAddress());
        lead.setIndustryCode(company.industryCode());
        lead.setReklamebeskyttet(company.reklamebeskyttet());
        lead.setCvrSyncedAt(Instant.now());
        return LeadResponse.from(leadRepository.save(lead));
    }

    /**
     * Soft delete (GDPR-design, data-protection.md): sætter deleted_at nu;
     * et senere purge-job hard-sletter efter grace-perioden.
     */
    @Transactional
    public void softDelete(Long id) {
        Lead lead = get(id);
        lead.setDeletedAt(Instant.now());
        leadRepository.save(lead);
    }
}
