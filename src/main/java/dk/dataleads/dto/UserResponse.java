package dk.dataleads.dto;

import dk.dataleads.domain.AppUser;
import dk.dataleads.domain.Role;

/**
 * Bruger-objektet vi sender UD. Indeholder BEVIDST ikke password_hash —
 * DTO'en er API-kontrakten, ikke databasen.
 */
public record UserResponse(
        Long id,
        String email,
        Role role
) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
