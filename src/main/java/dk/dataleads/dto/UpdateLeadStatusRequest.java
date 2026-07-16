package dk.dataleads.dto;

import dk.dataleads.domain.LeadStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request-body for statusskift på et lead. Noten er valgfri og gemmes på
 * den LeadActivity, der dokumenterer skiftet.
 */
public record UpdateLeadStatusRequest(
        @NotNull(message = "Status is required")
        LeadStatus status,

        String note
) {
}
