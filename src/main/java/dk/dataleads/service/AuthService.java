package dk.dataleads.service;

import dk.dataleads.domain.AppUser;
import dk.dataleads.domain.Role;
import dk.dataleads.dto.RegisterRequest;
import dk.dataleads.dto.UserResponse;
import dk.dataleads.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Registrering af nye brugere (ADR-0003). Adgangskoden hashes med den
 * konfigurerede PasswordEncoder (BCrypt) — klartekst forlader aldrig denne
 * metode. Login/session håndteres i AuthController (web-/session-anliggende).
 */
@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Opretter en bruger (rolle USER). Eksisterende email -> 409. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A user with that email already exists");
        }
        AppUser user = new AppUser(request.email(),
                passwordEncoder.encode(request.password()), Role.USER);
        return UserResponse.from(userRepository.save(user));
    }

    /** Slår den aktuelt indloggede bruger op på email (fra sessionen). */
    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Not authenticated"));
        return UserResponse.from(user);
    }
}
