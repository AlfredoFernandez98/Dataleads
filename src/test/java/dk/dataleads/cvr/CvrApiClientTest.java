package dk.dataleads.cvr;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit-test af CvrApiClient uden netværk og uden Spring-kontekst:
 * MockRestServiceServer bindes til RestClient-builderen, så vi kan teste
 * URL-form, User-Agent, JSON-mapping og retry-politikken (ADR-0002).
 */
class CvrApiClientTest {

    private static final String USER_AGENT = "Dataleads/0.1 (contact: af@zrm.dk)";
    private static final String LOOKUP_URL =
            "https://cvrapi.dk/api?search=12345678&country=dk&format=json";

    private static final String COMPANY_JSON = """
            {
              "vat": 12345678,
              "name": "Testfirma ApS",
              "address": "Testvej 1",
              "zipcode": "8000",
              "city": "Aarhus C",
              "protected": true,
              "phone": "12345678",
              "industrycode": 620100,
              "industrydesc": "Computerprogrammering"
            }""";

    private MockRestServiceServer server;

    /** Klient med mocket HTTP-lag; apiKey er null medmindre andet angives. */
    private CvrApiClient client(String apiKey) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        CvrProperties properties = new CvrProperties(
                "https://cvrapi.dk/api", apiKey, "dk", USER_AGENT,
                Duration.ofSeconds(2), Duration.ofSeconds(5), 2, 30);
        return new CvrApiClient(properties, builder);
    }

    @Test
    void lookupMapsJsonToCvrCompanyAndSendsRequiredUserAgent() {
        CvrApiClient client = client(null);
        server.expect(requestTo(LOOKUP_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withSuccess(COMPANY_JSON, MediaType.APPLICATION_JSON));

        Optional<CvrCompany> result = client.lookup("12345678");

        assertThat(result).isPresent();
        CvrCompany company = result.get();
        assertThat(company.cvr()).isEqualTo("12345678");
        assertThat(company.name()).isEqualTo("Testfirma ApS");
        assertThat(company.address()).isEqualTo("Testvej 1");
        assertThat(company.zipcode()).isEqualTo("8000");
        assertThat(company.city()).isEqualTo("Aarhus C");
        assertThat(company.industryCode()).isEqualTo("620100");
        assertThat(company.reklamebeskyttet()).isTrue();
        assertThat(company.fullAddress()).isEqualTo("Testvej 1, 8000 Aarhus C");
        server.verify();
    }

    @Test
    void lookupAppendsTokenOnlyWhenApiKeyIsConfigured() {
        CvrApiClient client = client("secret-key");
        server.expect(requestTo(LOOKUP_URL + "&token=secret-key"))
                .andRespond(withSuccess(COMPANY_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.lookup("12345678")).isPresent();
        server.verify();
    }

    @Test
    void lookupReturnsEmptyOnNotFound() {
        CvrApiClient client = client(null);
        server.expect(requestTo(LOOKUP_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"NOT_FOUND\"}"));

        assertThat(client.lookup("12345678")).isEmpty();
        server.verify();
    }

    @Test
    void lookupRetriesOn5xxAndSucceedsOnSecondAttempt() {
        CvrApiClient client = client(null);
        server.expect(requestTo(LOOKUP_URL)).andRespond(withServerError());
        server.expect(requestTo(LOOKUP_URL))
                .andRespond(withSuccess(COMPANY_JSON, MediaType.APPLICATION_JSON));

        Optional<CvrCompany> result = client.lookup("12345678");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Testfirma ApS");
        server.verify();
    }

    @Test
    void lookupRespectsRetryAfterOn429AndRetries() {
        CvrApiClient client = client(null);
        server.expect(requestTo(LOOKUP_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header(HttpHeaders.RETRY_AFTER, "0"));
        server.expect(requestTo(LOOKUP_URL))
                .andRespond(withSuccess(COMPANY_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.lookup("12345678")).isPresent();
        server.verify();
    }

    @Test
    void lookupThrows502AfterExhaustingRetriesOn5xx() {
        CvrApiClient client = client(null);
        // maxRetries = 2 -> 1 kald + 2 gen-forsøg = 3 kald i alt.
        server.expect(requestTo(LOOKUP_URL)).andRespond(withServerError());
        server.expect(requestTo(LOOKUP_URL)).andRespond(withServerError());
        server.expect(requestTo(LOOKUP_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.lookup("12345678"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
        server.verify();
    }

    @Test
    void lookupDoesNotRetryOnOther4xx() {
        CvrApiClient client = client(null);
        server.expect(requestTo(LOOKUP_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.lookup("12345678"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
        server.verify();
    }
}
