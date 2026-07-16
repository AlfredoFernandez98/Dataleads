package dk.dataleads.repository;

import dk.dataleads.domain.Lead;
import dk.dataleads.domain.LeadStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data-adgang for Lead. Alle læse-metoder filtrerer soft-deleted rækker fra
 * ('DeletedAtIsNull') — slettede leads findes stadig i tabellen indtil
 * purge-jobbet fjerner dem, men API'et må aldrig se dem.
 */
public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCvrAndDeletedAtIsNull(String cvr);

    Page<Lead> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Lead> findAllByStatusAndDeletedAtIsNull(LeadStatus status, Pageable pageable);
}
