package dk.dataleads.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registreringsanmodning (ADR-0003). Adgangskoden valideres kun på længde her;
 * den hashes med BCrypt i AuthService og lagres aldrig i klartekst.
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}
