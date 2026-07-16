-- V1: Lead + LeadActivity (ADR-0004, docs/design/data-model.md).
-- Flyway ejer skemaet i ALLE miljøer — entities må aldrig lande uden migrering.

CREATE TABLE lead (
    id                bigserial PRIMARY KEY,
    cvr               varchar(8)  NOT NULL,
    name              varchar     NOT NULL,
    -- varchar + CHECK i stedet for Postgres-enum (ADR-0004 §3):
    -- en ny status er én lille migrering, ikke en ALTER TYPE-øvelse.
    status            varchar     NOT NULL
        CONSTRAINT chk_lead_status
        CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'WON', 'LOST')),
    address           varchar,
    industry_code     varchar,
    -- Reklamebeskyttelse fra CVR — SKAL gemmes og respekteres i al outreach
    -- (docs/design/data-protection.md).
    reklamebeskyttet  boolean     NOT NULL DEFAULT false,
    cvr_synced_at     timestamptz,
    -- Soft delete: purge-job hard-sletter efter grace period (GDPR-erasure).
    deleted_at        timestamptz,
    version           bigint      NOT NULL DEFAULT 0,
    -- Nullable og uden FK endnu: User-tabellen findes først i auth-fasen.
    created_by        bigint,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    -- Single-user MVP: global unikhed pr. CVR. Bliver UNIQUE(owner_id, cvr)
    -- hvis multi-user nogensinde kommer (ADR-0004 §4).
    CONSTRAINT uq_lead_cvr UNIQUE (cvr)
);

CREATE INDEX idx_lead_status ON lead (status);
CREATE INDEX idx_lead_deleted_at ON lead (deleted_at);

CREATE TABLE lead_activity (
    id          bigserial PRIMARY KEY,
    lead_id     bigint      NOT NULL REFERENCES lead (id) ON DELETE CASCADE,
    type        varchar     NOT NULL
        CONSTRAINT chk_lead_activity_type
        CHECK (type IN ('STATUS_CHANGE', 'NOTE')),
    old_status  varchar,
    new_status  varchar,
    -- Fritekst — kan indeholde persondata; purges sammen med leadet.
    note        text,
    created_by  bigint,
    -- Append-only historik: ingen updated_at/version.
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_lead_activity_lead_id ON lead_activity (lead_id);
