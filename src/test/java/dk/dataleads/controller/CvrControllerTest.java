package dk.dataleads.controller;

import dk.dataleads.config.SecurityConfig;
import dk.dataleads.cvr.CvrClient;
import dk.dataleads.cvr.CvrCompany;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice-test af de rene CVR-opslag (/api/v1/cvr). CvrClient mockes —
 * vi tester HTTP-kontrakten: 200 ved fund, 404 ved ukendt CVR, 400 ved
 * ugyldigt format (alle fejl som ProblemDetail). SecurityConfig importeres,
 * så testen også beviser at /api/v1/cvr/** er åbent (TEMP, ADR-0003).
 */
@WebMvcTest(CvrController.class)
@Import(SecurityConfig.class)
class CvrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CvrClient cvrClient;

    private static CvrCompany sampleCompany() {
        return new CvrCompany("12345678", "Testfirma ApS", "Testvej 1",
                "8000", "Aarhus C", "620100", true);
    }

    @Test
    void lookupReturns200WithCompany() throws Exception {
        when(cvrClient.lookup("12345678")).thenReturn(Optional.of(sampleCompany()));

        mockMvc.perform(get("/api/v1/cvr/12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cvr").value("12345678"))
                .andExpect(jsonPath("$.name").value("Testfirma ApS"))
                .andExpect(jsonPath("$.reklamebeskyttet").value(true));
    }

    @Test
    void lookupReturns404ProblemDetailWhenUnknown() throws Exception {
        when(cvrClient.lookup("12345678")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/cvr/12345678"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void lookupReturns400ProblemDetailOnInvalidCvrFormat() throws Exception {
        mockMvc.perform(get("/api/v1/cvr/12AB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("CVR must be exactly 8 digits"));
    }
}
