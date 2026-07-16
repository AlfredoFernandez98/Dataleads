# Dataleads

A lead-generation tool for the Danish market: look companies up in the **CVR** register,
manage them as leads, and (later) bill via **Stripe**. Backend is Spring Boot; a React/Vite
frontend follows in a later phase.

## Tech stack
- Java 23, Spring Boot 4.1 (webmvc, data-jpa, validation)
- PostgreSQL (run locally with Docker)
- Maven (use the wrapper `./mvnw`)

## Prerequisites
- JDK 23
- Docker (for the local database)

## Getting started
```bash
# 1. Configure environment
cp .env.example .env        # then fill in real values (never committed)

# 2. Start the database
docker compose up -d db

# 3. Run the app (dev profile by default)
./mvnw spring-boot:run

# 4. Run tests
./mvnw test
```

## Environment variables
Secrets come from `.env` (gitignored). See `.env.example` for the full list:

| Variable | Description |
|---|---|
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | PostgreSQL database and credentials |
| `CVR_API_KEY` | Key for the CVR company-lookup API |
| `STRIPE_SECRET_KEY` | Stripe secret key (phase 4) |

## Profiles / environments
Select with `SPRING_PROFILES_ACTIVE`:

| Profile | Use | Schema handling |
|---|---|---|
| `dev` (default) | local development | `ddl-auto=update`, verbose SQL |
| `staging` | test environment | `ddl-auto=validate` |
| `prod` | production | `ddl-auto=validate` (Flyway migrations planned) |

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

## Branching model
- `main` — production (only tested code).
- `develop` — daily integration branch.
- `feature/<name>` — one change at a time, merged into `develop`.

Commits follow [Conventional Commits](https://www.conventionalcommits.org/)
(`feat:`, `fix:`, `chore:`, `docs:`, …).

## Security
No secrets in the repo. Configuration is injected via environment variables / `.env`, which is
gitignored. Only `.env.example` (placeholders) is committed.
