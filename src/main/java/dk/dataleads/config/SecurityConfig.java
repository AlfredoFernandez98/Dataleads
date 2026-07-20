package dk.dataleads.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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
                // CSRF slået til med en cookie-baseret token (ADR-0003): SPA'en læser
                // XSRF-TOKEN-cookien (withHttpOnlyFalse) og sender den tilbage som
                // X-XSRF-TOKEN-header på muterende kald. Session-cookie-auth kræver dette.
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                // CORS-reglerne kommer fra corsConfigurationSource()-beanen nedenfor.
                .cors(Customizer.withDefaults())
                // Session oprettes efter behov (ved login) — det er bæreren af auth.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                        // Åbne auth-endpoints: opret bruger + login. Resten kræver session.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        // Alt andet — inkl. /api/v1/leads/** og /api/v1/cvr/** — kræver login.
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
     * BCrypt jf. ADR-0003. Sammen med AppUserDetailsService-beanen får Spring
     * Boot auto-konfigureret en DaoAuthenticationProvider, som login bruger.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Eksponerer AuthenticationManager, så AuthController kan autentificere login. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
