package dk.dataleads.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request-body for oprettelse af et lead. Bean Validation (@Valid i
 * controlleren) afviser ugyldige felter som 400 ProblemDetail, før
 * servicen overhovedet kaldes.
 */
public record CreateLeadRequest(
        @NotBlank(message = "CVR is required")
        @Pattern(regexp = "\\d{8}", message = "CVR must be exactly 8 digits")
        String cvr,

        @NotBlank(message = "Name is required")
        String name,

        String address,

        String industryCode,

        boolean reklamebeskyttet
) {
}
