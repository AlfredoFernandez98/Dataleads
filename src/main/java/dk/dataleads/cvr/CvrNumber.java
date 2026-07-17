package dk.dataleads.cvr;

import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fælles validering af CVR-numre i path-variabler (controllere kan ikke
 * bruge @Valid på en @PathVariable-String uden method validation-setup,
 * så vi kaster selv 400 som ProblemDetail via GlobalExceptionHandler).
 */
public final class CvrNumber {

    private static final Pattern CVR_PATTERN = Pattern.compile("\\d{8}");

    private CvrNumber() {
    }

    /** Kaster 400 Bad Request hvis strengen ikke er præcis 8 cifre. */
    public static void requireValid(String cvr) {
        if (cvr == null || !CVR_PATTERN.matcher(cvr).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CVR must be exactly 8 digits");
        }
    }
}
