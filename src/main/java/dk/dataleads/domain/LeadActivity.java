package dk.dataleads.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Append-only historik på et lead: statusskift og noter
 * (docs/design/data-model.md). Ingen updated_at/@Version — rækker ændres
 * aldrig efter oprettelse. Slettes med leadet (ON DELETE CASCADE).
 */
@Entity
@Table(name = "lead_activity")
public class LeadActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadActivityType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private LeadStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status")
    private LeadStatus newStatus;

    /** Fritekst — kan indeholde persondata; purges sammen med leadet. */
    @Column(columnDefinition = "text")
    private String note;

    /** Nullable brugerreference indtil auth-fasen — ingen FK endnu. */
    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA kræver en no-arg constructor; protected så den ikke misbruges. */
    protected LeadActivity() {
    }

    private LeadActivity(Lead lead, LeadActivityType type, LeadStatus oldStatus, LeadStatus newStatus, String note) {
        this.lead = lead;
        this.type = type;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.note = note;
    }

    /** Fabrik for et statusskift (med valgfri note). */
    public static LeadActivity statusChange(Lead lead, LeadStatus oldStatus, LeadStatus newStatus, String note) {
        return new LeadActivity(lead, LeadActivityType.STATUS_CHANGE, oldStatus, newStatus, note);
    }

    /** Fabrik for en fritstående note. */
    public static LeadActivity note(Lead lead, String note) {
        return new LeadActivity(lead, LeadActivityType.NOTE, null, null, note);
    }

    public Long getId() {
        return id;
    }

    public Lead getLead() {
        return lead;
    }

    public LeadActivityType getType() {
        return type;
    }

    public LeadStatus getOldStatus() {
        return oldStatus;
    }

    public LeadStatus getNewStatus() {
        return newStatus;
    }

    public String getNote() {
        return note;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
