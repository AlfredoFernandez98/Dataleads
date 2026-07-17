package dk.dataleads.cvr;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed config for CVR-integrationen (ADR-0002). Bindes fra 'cvr.*'-keys og
 * aktiveres via @ConfigurationPropertiesScan på DataleadsApplication.
 * Auth-formen er bevidst løs (valgfri api-key nu; Virk bruger user/password
 * senere) — kilden skiftes via config, ikke kode.
 *
 * @param baseUrl          cvrapi.dk-endpointet (uden query-parametre)
 * @param apiKey           valgfri token fra cvrapi.dk; null/tom = anonym adgang
 * @param country          'dk' (cvrapi.dk understøtter også 'no')
 * @param userAgent        cvrapi.dk KRÆVER en beskrivende User-Agent med kontaktinfo
 * @param connectTimeout   TCP-connect-timeout (ADR-0002: ~2 s)
 * @param readTimeout      læse-timeout (ADR-0002: ~5 s)
 * @param maxRetries       antal GEN-forsøg oven i første kald (kun 5xx/timeout/429)
 * @param cacheRefreshDays data ældre end dette regnes som forældet (cvr_synced_at)
 */
@ConfigurationProperties(prefix = "cvr")
public record CvrProperties(
        @DefaultValue("https://cvrapi.dk/api") String baseUrl,
        String apiKey,
        @DefaultValue("dk") String country,
        @DefaultValue("Dataleads/0.1 (contact: af@zrm.dk)") String userAgent,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout,
        @DefaultValue("2") int maxRetries,
        @DefaultValue("30") int cacheRefreshDays
) {
}
