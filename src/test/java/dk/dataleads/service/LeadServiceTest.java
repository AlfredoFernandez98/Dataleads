package dk.dataleads.service;

import dk.dataleads.domain.Lead;
import dk.dataleads.domain.LeadActivity;
import dk.dataleads.domain.LeadActivityType;
import dk.dataleads.domain.LeadStatus;
import dk.dataleads.dto.CreateLeadRequest;
import dk.dataleads.dto.UpdateLeadStatusRequest;
import dk.dataleads.repository.LeadActivityRepository;
import dk.dataleads.repository.LeadRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Rene Mockito-unit-tests af LeadService: ingen Spring-kontekst, ingen
 * database — repositories mockes, og vi tester KUN forretningslogikken
 * (dublet-afvisning, 404, aktivitetslog, soft delete).
 */
@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadActivityRepository leadActivityRepository;

    @InjectMocks
    private LeadService leadService;

    private static Lead sampleLead() {
        return new Lead("12345678", "Testfirma ApS", "Testvej 1, 8000 Aarhus C", "620100", false);
    }

    @Test
    void createRejectsDuplicateCvrWith409() {
        when(leadRepository.existsByCvrAndDeletedAtIsNull("12345678")).thenReturn(true);

        CreateLeadRequest request = new CreateLeadRequest(
                "12345678", "Testfirma ApS", null, null, false);

        assertThatThrownBy(() -> leadService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(leadRepository, never()).save(any());
    }

    @Test
    void createSavesNewLeadWithStatusNew() {
        when(leadRepository.existsByCvrAndDeletedAtIsNull("12345678")).thenReturn(false);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        Lead created = leadService.create(new CreateLeadRequest(
                "12345678", "Testfirma ApS", "Testvej 1", "620100", true));

        assertThat(created.getCvr()).isEqualTo("12345678");
        assertThat(created.getStatus()).isEqualTo(LeadStatus.NEW);
        assertThat(created.isReklamebeskyttet()).isTrue();
    }

    @Test
    void getMissingLeadThrows404() {
        when(leadRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.get(42L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateStatusChangesStatusAndRecordsActivity() {
        Lead lead = sampleLead();
        when(leadRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        Lead updated = leadService.updateStatus(1L,
                new UpdateLeadStatusRequest(LeadStatus.CONTACTED, "Ringede kl. 10"));

        assertThat(updated.getStatus()).isEqualTo(LeadStatus.CONTACTED);

        ArgumentCaptor<LeadActivity> captor = ArgumentCaptor.forClass(LeadActivity.class);
        verify(leadActivityRepository).save(captor.capture());
        LeadActivity activity = captor.getValue();
        assertThat(activity.getType()).isEqualTo(LeadActivityType.STATUS_CHANGE);
        assertThat(activity.getOldStatus()).isEqualTo(LeadStatus.NEW);
        assertThat(activity.getNewStatus()).isEqualTo(LeadStatus.CONTACTED);
        assertThat(activity.getNote()).isEqualTo("Ringede kl. 10");
        assertThat(activity.getLead()).isSameAs(updated);
    }

    @Test
    void softDeleteSetsDeletedAt() {
        Lead lead = sampleLead();
        when(leadRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        leadService.softDelete(1L);

        assertThat(lead.getDeletedAt()).isNotNull();
        verify(leadRepository).save(lead);
    }

    @Test
    void softDeleteMissingLeadThrows404() {
        when(leadRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.softDelete(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
