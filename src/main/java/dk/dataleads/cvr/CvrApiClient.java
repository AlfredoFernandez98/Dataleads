package dk.dataleads.cvr;

import java.net.http.HttpClient;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * CvrClient mod cvrapi.dk (ADR-0002, MVP-kilden). Kalder
 * GET {baseUrl}?search={cvr}&country={country}&format=json (+ token hvis
 * api-key er sat) med den obligatoriske beskrivende User-Agent.
 *
 * Fejlpolitik (ADR-0002):
 * - 404 (ukendt CVR)  -> Optional.empty() — ikke en fejl.
 * - 429               -> respekter Retry-After, retry op til maxRetries.
 * - 5xx / timeout     -> eksponentiel backoff + jitter, retry op til maxRetries.
 * - Øvrige 4xx        -> ingen retry (vores fejl, ikke kildens).
 * - Endelig fiasko    -> 502 Bad Gateway som ProblemDetail.
 * Retry-logikken er bevidst en lille privat helper — ingen spring-retry/
 * resilience4j-afhængighed for tre catch-grene.
 */
@Component
public class CvrApiClient implements CvrClient {

    private static final Logger log = LoggerFactory.getLogger(CvrApiClient.class);

    /** Basis for eksponentiel backoff: 250 ms, 500 ms, 1 s, ... (+ jitter). */
    private static final long BACKOFF_BASE_MILLIS = 250;

    /** Loft over hvor længe vi vil sove på en request-tråd (også for Retry-After). */
    private static final long MAX_SLEEP_MILLIS = 5_000;

    private final CvrProperties properties;
    private final RestClient restClient;

    @Autowired
    public CvrApiClient(CvrProperties properties) {
        this(properties, RestClient.builder().requestFactory(requestFactory(properties)));
    }

    /**
     * Package-private til tests: builderen kan være bundet til en
     * MockRestServiceServer (som selv sætter request factory).
     */
    CvrApiClient(CvrProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .build();
    }

    /** JDK-HttpClient med ADR-0002-timeouts: connect ~2 s, read ~5 s. */
    private static JdkClientHttpRequestFactory requestFactory(CvrProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }

    @Override
    public Optional<CvrCompany> lookup(String cvr) {
        for (int attempt = 0; ; attempt++) {
            try {
                CvrApiResponse body = fetch(cvr);
                if (body == null || body.vat() == null) {
                    // Defensivt: kilden har svaret 200 uden et firma.
                    return Optional.empty();
                }
                return Optional.of(toCompany(cvr, body));
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status == HttpStatus.NOT_FOUND.value()) {
                    return Optional.empty();
                }
                boolean retryable = status == HttpStatus.TOO_MANY_REQUESTS.value()
                        || e.getStatusCode().is5xxServerError();
                if (!retryable || attempt >= properties.maxRetries()) {
                    throw upstreamFailure(cvr, "HTTP " + status, e);
                }
                int currentAttempt = attempt;
                long delay = status == HttpStatus.TOO_MANY_REQUESTS.value()
                        ? retryAfterMillis(e).orElseGet(() -> backoffMillis(currentAttempt))
                        : backoffMillis(currentAttempt);
                log.warn("CVR lookup for {} got HTTP {}; retrying in {} ms (attempt {}/{})",
                        cvr, status, delay, attempt + 1, properties.maxRetries());
                sleep(delay);
            } catch (ResourceAccessException e) {
                // Timeout / connection refused — retry som 5xx (idempotent GET).
                if (attempt >= properties.maxRetries()) {
                    throw upstreamFailure(cvr, "I/O error", e);
                }
                long delay = backoffMillis(attempt);
                log.warn("CVR lookup for {} failed with I/O error; retrying in {} ms (attempt {}/{})",
                        cvr, delay, attempt + 1, properties.maxRetries());
                sleep(delay);
            }
        }
    }

    private CvrApiResponse fetch(String cvr) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.queryParam("search", cvr)
                            .queryParam("country", properties.country())
                            .queryParam("format", "json");
                    if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
                        uriBuilder.queryParam("token", properties.apiKey());
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(CvrApiResponse.class);
    }

    /** Rå cvrapi.dk-JSON -> vores interne model. CVR-input beholdes som kanonisk nøgle. */
    private static CvrCompany toCompany(String cvr, CvrApiResponse body) {
        return new CvrCompany(
                cvr,
                body.name(),
                body.address(),
                body.zipcode(),
                body.city(),
                body.industrycode() != null ? String.valueOf(body.industrycode()) : null,
                Boolean.TRUE.equals(body.protectedStatus()));
    }

    /** Retry-After i sekunder (delta-seconds-formen); dato-formen falder tilbage til backoff. */
    private static Optional<Long> retryAfterMillis(RestClientResponseException e) {
        String retryAfter = e.getResponseHeaders() != null
                ? e.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER)
                : null;
        if (retryAfter == null) {
            return Optional.empty();
        }
        try {
            long seconds = Long.parseLong(retryAfter.trim());
            return Optional.of(Math.min(seconds * 1_000, MAX_SLEEP_MILLIS));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /** Eksponentiel backoff med jitter: 250, 500, 1000 ms ... + 0-100 ms. */
    private static long backoffMillis(int attempt) {
        long base = BACKOFF_BASE_MILLIS << attempt;
        long jitter = ThreadLocalRandom.current().nextLong(100);
        return Math.min(base + jitter, MAX_SLEEP_MILLIS);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "CVR lookup was interrupted while waiting to retry");
        }
    }

    private static ResponseStatusException upstreamFailure(String cvr, String cause, Exception e) {
        log.error("CVR lookup for {} failed permanently ({})", cvr, cause, e);
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "CVR registry is currently unavailable — please try again later");
    }
}
