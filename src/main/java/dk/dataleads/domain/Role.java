package dk.dataleads.domain;

/**
 * Brugerrolle (ADR-0003). Mappes til Spring Security-authority ROLE_&lt;navn&gt;
 * i AppUserDetailsService. varchar + CHECK i skemaet (ADR-0004 §3).
 */
public enum Role {
    USER,
    ADMIN
}
