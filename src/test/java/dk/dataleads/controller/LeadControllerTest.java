package dk.dataleads.controller;

import dk.dataleads.config.SecurityConfig;
import dk.dataleads.domain.Lead;
import dk.dataleads.dto.CreateLeadRequest;
import dk.dataleads.dto.LeadResponse;
import dk.dataleads.service.LeadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice-test: kun controller-laget + security-reglerne. LeadService
 * mockes — vi tester HTTP-kontrakten (status-koder, validering som
 * ProblemDetail, Location-header), ikke forretningslogikken.
 * SecurityConfig importeres, så testen også beviser at /api/v1/leads/**
 * er åbent (TEMP indtil auth-fasen, ADR-0003).
 */
@WebMvcTest(LeadController.class)
@Import(SecurityConfig.class)
class LeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeadService leadService;

    private static Lead leadWithId(long id) {
        Lead lead = new Lead("12345678", "Testfirma ApS", "Testvej 1, 8000 Aarhus C", "620100", false);
        ReflectionTestUtils.setField(lead, "id", id);
        return lead;
    }

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        when(leadService.create(any(CreateLeadRequest.class))).thenReturn(leadWithId(7L));

        mockMvc.perform(post("/api/v1/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cvr":"12345678","name":"Testfirma ApS",
                                 "address":"Testvej 1, 8000 Aarhus C","industryCode":"620100",
                                 "reklamebeskyttet":false}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/leads/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.cvr").value("12345678"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void createWithBlankCvrReturns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cvr":"","name":"Testfirma ApS","reklamebeskyttet":false}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.cvr").exists());
    }

    @Test
    void createWithInvalidCvrFormatReturns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cvr":"12AB","name":"Testfirma ApS","reklamebeskyttet":false}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cvr").value("CVR must be exactly 8 digits"));
    }

    @Test
    void getMissingLeadReturns404ProblemDetail() throws Exception {
        when(leadService.get(eq(99L))).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead 99 not found"));

        mockMvc.perform(get("/api/v1/leads/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Lead 99 not found"));
    }

    @Test
    void importFromCvrReturns201WithLocation() throws Exception {
        when(leadService.importFromCvr("12345678")).thenReturn(LeadResponse.from(leadWithId(7L)));

        mockMvc.perform(post("/api/v1/leads/from-cvr/12345678"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/leads/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.cvr").value("12345678"));
    }

    @Test
    void importFromCvrWithInvalidCvrReturns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/leads/from-cvr/12AB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("CVR must be exactly 8 digits"));
    }
}
