package dk.dataleads.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global fejlhåndtering (RFC 9457 "Problem Details for HTTP APIs").
 * Alle fejl fra REST-laget bliver til et ensartet ProblemDetail-JSON-svar,
 * i stedet for Spring's default whitelabel/HTML eller rå stacktraces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean-validation-fejl (@Valid på request bodies) -> 400 med et
     * 'errors'-map: feltnavn -> besked, så klienten kan vise fejl pr. felt.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed for one or more fields.");
        problem.setTitle("Validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("errors", errors);
        return problem;
    }

    /**
     * Ulæselig/misdannet request body (fx ugyldig JSON eller null i et
     * primitivt felt) -> 400. En klientfejl må aldrig blive til en 500.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request body is missing or malformed.");
        problem.setTitle("Malformed request body");
        return problem;
    }

    /**
     * Bevidst kastede statusfejl (fx 'throw new ResponseStatusException(NOT_FOUND, ...)')
     * passerer igennem med deres egen status og reason.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        String detail = ex.getReason() != null ? ex.getReason() : "Request could not be completed.";
        return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), detail);
    }

    /**
     * Fallback: alt andet -> 500 med en GENERISK besked. Vi lækker aldrig
     * interne detaljer (exception-klasse, SQL, stacktrace) til klienten —
     * de logges i stedet på error-niveau.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) throws Exception {
        // Auth-fejl skal håndteres af Spring Security (401/403), ikke blive til 500.
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        log.error("Unhandled exception while processing request", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred.");
    }
}
