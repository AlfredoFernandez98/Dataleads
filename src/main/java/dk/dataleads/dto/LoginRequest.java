package dk.dataleads.dto;

import jakarta.validation.constraints.NotBlank;

/** Login-anmodning (ADR-0003). Autentificeres via AuthenticationManager. */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
