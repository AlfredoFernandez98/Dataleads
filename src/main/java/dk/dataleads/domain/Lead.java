package dk.dataleads.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Et firma vi tracker som potentiel kunde (docs/design/data-model.md).
 * Skemaet ejes af Flyway (V1__init.sql) — entiteten skal blot matche det
 * (ddl-auto=validate). Single-user MVP: ingen owner_id endnu (ADR-0004 §4).
 */
@Entity
@Table(name = "lead")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dansk CVR-nummer — det naturlige, offentlige nøglefelt for et lead. */
    @Column(nullable = false, length = 8, unique = true)
    private String cvr;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status = LeadStatus.NEW;

    private String address;

    @Column(name = "industry_code")
    private String industryCode;

    /**
     * Reklamebeskyttelse fra CVR: må IKKE cold-kontaktes. Skal filtreres/
     * advares på i enhver outreach-feature (docs/design/data-protection.md).
     */
    @Column(nullable = false)
    private boolean reklamebeskyttet;

    /** Sidste CVR-opfriskning; forældet efter 30 dage (ADR-0002). */
    @Column(name = "cvr_synced_at")
    private Instant cvrSyncedAt;

    /** Soft delete: sat = slettet; purge-job hard-sletter efter grace period. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Optimistisk låsning (ADR-0004 §5). */
    @Version
    @Column(nullable = false)
    private long version;

    /** Nullable brugerreference indtil auth-fasen — ingen FK endnu. */
    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA kræver en no-arg constructor; protected så den ikke misbruges. */
    protected Lead() {
    }

    public Lead(String cvr, String name, String address, String industryCode, boolean reklamebeskyttet) {
        this.cvr = cvr;
        this.name = name;
        this.address = address;
        this.industryCode = industryCode;
        this.reklamebeskyttet = reklamebeskyttet;
        this.status = LeadStatus.NEW;
    }

    public Long getId() {
        return id;
    }

    public String getCvr() {
        return cvr;
    }

    public void setCvr(String cvr) {
        this.cvr = cvr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public void setIndustryCode(String industryCode) {
        this.industryCode = industryCode;
    }

    public boolean isReklamebeskyttet() {
        return reklamebeskyttet;
    }

    public void setReklamebeskyttet(boolean reklamebeskyttet) {
        this.reklamebeskyttet = reklamebeskyttet;
    }

    public Instant getCvrSyncedAt() {
        return cvrSyncedAt;
    }

    public void setCvrSyncedAt(Instant cvrSyncedAt) {
        this.cvrSyncedAt = cvrSyncedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public long getVersion() {
        return version;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
