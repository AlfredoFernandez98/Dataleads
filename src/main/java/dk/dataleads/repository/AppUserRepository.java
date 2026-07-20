package dk.dataleads.repository;

import dk.dataleads.domain.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA-repository for brugere. Email er den naturlige login-nøgle
 * (UNIQUE i skemaet).
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
