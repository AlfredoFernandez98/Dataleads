package dk.dataleads.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security-skelet jf. ADR-0003 (DIY email+password, HttpOnly session-cookies).
 * Der findes endnu INGEN brugere — dette er placeholder-konfigurationen:
 * health/info er åbne, alt andet kræver auth (og svarer 401 JSON, ikke en
 * login-side). Selve login-flowet kommer i auth-fasen.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final List<String> allowedOrigins;

    public SecurityConfig(@Value("${app.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // TODO(ADR-0003): når login/session-endpoints kommer, slå CSRF til med
                // CookieCsrfTokenRepository.withHttpOnlyFalse(), så SPA'en kan læse
                // XSRF-TOKEN-cookien og sende X-XSRF-TOKEN-headeren. Lige nu findes der
                // ingen browser-session-endpoints (kun åbne GET-health-endpoints), så
                // CSRF er deaktiveret som det simpleste korrekte valg.
                .csrf(csrf -> csrf.disable())
                // CORS-reglerne kommer fra corsConfigurationSource()-beanen nedenfor.
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                        // TEMP: lock down in the auth phase (ADR-0003) — der findes
                        // ingen brugere endnu, så lead-API'et er åbent indtil login lander.
                        .requestMatchers("/api/v1/leads/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                // API-adfærd: uautentificerede kald får 401 JSON — aldrig et redirect
                // til en login-side. Derfor er formLogin/httpBasic også slået fra.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }

    /**
     * 401-svar som RFC 9457 ProblemDetail-JSON i stedet for Spring's default
     * (redirect til login-side / WWW-Authenticate-popup).
     */
    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {"type":"about:blank","title":"Unauthorized","status":401,\
                    "detail":"Authentication is required to access this resource."}""");
        };
    }

    /**
     * CORS jf. ADR-0003: præcise origins fra 'app.cors.allowed-origins'
     * (default = Vite-dev-origin). ALDRIG '*' — og slet ikke sammen med
     * allowCredentials(true), som session-cookies kræver.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "X-XSRF-TOKEN", "X-Requested-With"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /**
     * BCrypt jf. ADR-0003 — klar til auth-fasen (User.password_hash).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
