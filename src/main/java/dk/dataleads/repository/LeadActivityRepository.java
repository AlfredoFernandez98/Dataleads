package dk.dataleads.repository;

import dk.dataleads.domain.LeadActivity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data-adgang for LeadActivity (append-only historik på et lead).
 */
public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {

    List<LeadActivity> findAllByLeadIdOrderByCreatedAtDesc(Long leadId);
}
