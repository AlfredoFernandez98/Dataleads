package dk.dataleads.dto;

import dk.dataleads.domain.Lead;
import dk.dataleads.domain.LeadStatus;
import java.time.Instant;

/**
 * Det lead-objekt vi sender UD til klienten. Bevidst uden interne felter
 * (deleted_at, created_by) — DTO'en er API-kontrakten, ikke databasen.
 * 'version' eksponeres, så klienten kan sende den med ved opdateringer
 * (optimistisk låsning, ADR-0004 §5).
 */
public record LeadResponse(
        Long id,
        String cvr,
        String name,
        LeadStatus status,
        String address,
        String industryCode,
        boolean reklamebeskyttet,
        Instant cvrSyncedAt,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    /** Statisk fabrik: entity -> DTO, samlet ét sted. */
    public static LeadResponse from(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getCvr(),
                lead.getName(),
                lead.getStatus(),
                lead.getAddress(),
                lead.getIndustryCode(),
                lead.isReklamebeskyttet(),
                lead.getCvrSyncedAt(),
                lead.getVersion(),
                lead.getCreatedAt(),
                lead.getUpdatedAt());
    }
}
