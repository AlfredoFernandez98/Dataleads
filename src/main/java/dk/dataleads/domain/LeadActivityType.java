package dk.dataleads.domain;

/**
 * Typen af en LeadActivity-post. Udvides senere (CALL, EMAIL, ...) — hver ny
 * type kræver også en migrering af CHECK-constrainten (ADR-0004 §3).
 */
public enum LeadActivityType {
    STATUS_CHANGE,
    NOTE
}
