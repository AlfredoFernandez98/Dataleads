-- V2: Authentication — app_user (ADR-0003, docs/design/data-model.md).
-- 'user' er et reserveret ord i Postgres, så tabellen hedder app_user.
-- Flyway ejer skemaet i ALLE miljøer (ddl-auto=validate).

CREATE TABLE app_user (
    id             bigserial PRIMARY KEY,
    email          varchar     NOT NULL,
    -- BCrypt-hash (ADR-0003) — ALDRIG klartekst, aldrig eksponeret i API'et.
    password_hash  varchar     NOT NULL,
    role           varchar     NOT NULL
        CONSTRAINT chk_app_user_role
        CHECK (role IN ('USER', 'ADMIN')),
    version        bigint      NOT NULL DEFAULT 0,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    -- Email er den naturlige login-nøgle; UNIQUE dækker også opslags-indekset.
    CONSTRAINT uq_app_user_email UNIQUE (email)
);
