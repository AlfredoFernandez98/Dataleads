package dk.dataleads;

import dk.dataleads.controller.HealthController;
import dk.dataleads.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Slice"-test: @WebMvcTest starter KUN web-laget (controlleren),
 * ikke hele appen og ingen database. Derfor er den lynhurtig.
 * @Import trækker den rigtige HealthService ind, så vi tester kæden
 * controller -> service uden at mocke.
 */
@WebMvcTest(HealthController.class)
@Import(HealthService.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.app").value("dataleads"));
    }
}
