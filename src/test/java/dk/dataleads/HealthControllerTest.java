package dk.dataleads;

import dk.dataleads.config.SecurityConfig;
import dk.dataleads.controller.HealthController;
import dk.dataleads.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Slice"-test: @WebMvcTest starter KUN web-laget (controlleren),
 * ikke hele appen og ingen database. Derfor er den lynhurtig.
 * @Import trækker den rigtige HealthService + SecurityConfig ind, så vi
 * tester kæden controller -> service MED security-reglerne aktive.
 * Actuator's HealthEndpoint findes ikke i web-slicen, så den mockes
 * til at svare UP — det er den eneste grænse vi stuber.
 */
@WebMvcTest(HealthController.class)
@Import({HealthService.class, SecurityConfig.class})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthEndpoint healthEndpoint;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        // HealthDescriptor er sealed, så vi mocker den konkrete (final) subklasse.
        IndicatedHealthDescriptor descriptor = mock(IndicatedHealthDescriptor.class);
        when(descriptor.getStatus()).thenReturn(Status.UP);
        when(healthEndpoint.health()).thenReturn(descriptor);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.app").value("dataleads"));
    }

    @Test
    void healthEndpointReturns503WhenDown() throws Exception {
        IndicatedHealthDescriptor descriptor = mock(IndicatedHealthDescriptor.class);
        when(descriptor.getStatus()).thenReturn(Status.DOWN);
        when(healthEndpoint.health()).thenReturn(descriptor);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }
}
