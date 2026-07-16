package dk.dataleads.repository;

import dk.dataleads.TestcontainersConfiguration;
import dk.dataleads.domain.Lead;
import dk.dataleads.domain.LeadStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-slice-test mod en RIGTIG PostgreSQL i Docker (Testcontainers).
 * replace = NONE: brug containeren fra TestcontainersConfiguration i stedet
 * for en embedded database — så kører Flyway-migreringerne (V1__init.sql)
 * også, og vi tester det ægte skema inkl. constraints. Kræver Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class LeadRepositoryTest {

    @Autowired
    private LeadRepository leadRepository;

    private Lead persist(String cvr, String name) {
        return leadRepository.saveAndFlush(new Lead(cvr, name, null, null, false));
    }

    @Test
    void findByIdAndDeletedAtIsNullReturnsActiveLead() {
        Lead saved = persist("11111111", "Aktiv ApS");

        assertThat(leadRepository.findByIdAndDeletedAtIsNull(saved.getId()))
                .hasValueSatisfying(lead -> {
                    assertThat(lead.getCvr()).isEqualTo("11111111");
                    assertThat(lead.getStatus()).isEqualTo(LeadStatus.NEW);
                    assertThat(lead.getCreatedAt()).isNotNull();
                    assertThat(lead.getUpdatedAt()).isNotNull();
                });
    }

    @Test
    void findByIdAndDeletedAtIsNullExcludesSoftDeletedLead() {
        Lead saved = persist("22222222", "Slettet ApS");
        saved.setDeletedAt(Instant.now());
        leadRepository.saveAndFlush(saved);

        assertThat(leadRepository.findByIdAndDeletedAtIsNull(saved.getId())).isEmpty();
    }

    @Test
    void existsByCvrAndDeletedAtIsNullRespectsSoftDelete() {
        Lead saved = persist("33333333", "Findes ApS");

        assertThat(leadRepository.existsByCvrAndDeletedAtIsNull("33333333")).isTrue();
        assertThat(leadRepository.existsByCvrAndDeletedAtIsNull("99999999")).isFalse();

        saved.setDeletedAt(Instant.now());
        leadRepository.saveAndFlush(saved);

        assertThat(leadRepository.existsByCvrAndDeletedAtIsNull("33333333")).isFalse();
    }

    @Test
    void findAllByDeletedAtIsNullPagesOnlyActiveLeads() {
        persist("44444444", "Aktiv 1 ApS");
        Lead deleted = persist("55555555", "Slettet 2 ApS");
        deleted.setDeletedAt(Instant.now());
        leadRepository.saveAndFlush(deleted);

        Page<Lead> page = leadRepository.findAllByDeletedAtIsNull(PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Lead::getCvr)
                .contains("44444444")
                .doesNotContain("55555555");
    }

    @Test
    void findAllByStatusAndDeletedAtIsNullFiltersOnStatus() {
        Lead contacted = persist("66666666", "Kontaktet ApS");
        contacted.setStatus(LeadStatus.CONTACTED);
        leadRepository.saveAndFlush(contacted);
        persist("77777777", "Ny ApS");

        Page<Lead> page = leadRepository.findAllByStatusAndDeletedAtIsNull(
                LeadStatus.CONTACTED, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Lead::getCvr)
                .contains("66666666")
                .doesNotContain("77777777");
    }
}
