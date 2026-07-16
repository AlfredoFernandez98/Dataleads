package dk.dataleads.domain;

/**
 * Salgsstatus for et lead. Gemmes som varchar + CHECK i databasen og mappes
 * med @Enumerated(EnumType.STRING) — ADR-0004 §3. En ny status kræver både
 * en værdi her OG en migrering, der udvider CHECK-constrainten.
 */
public enum LeadStatus {
    NEW,
    CONTACTED,
    QUALIFIED,
    WON,
    LOST
}
