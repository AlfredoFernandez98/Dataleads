package dk.dataleads.security;

import dk.dataleads.domain.AppUser;
import dk.dataleads.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Kobler vores AppUser-tabel sammen med Spring Security (ADR-0003):
 * slår brugeren op på email og mapper rollen til authority ROLE_&lt;navn&gt;.
 * BCrypt-verifikationen af password_hash sker i DaoAuthenticationProvider
 * via den registrerede PasswordEncoder.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public AppUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}
