# Dataleads

A lead-generation tool for the Danish market: look companies up in the **CVR** register,
manage them as leads, and (later) bill via **Stripe**. Backend is Spring Boot; a React/Vite
frontend follows in a later phase.

## Tech stack
- Java 21 (LTS), Spring Boot 4.1 (webmvc, data-jpa, validation)
- PostgreSQL (run locally with Docker)
- Maven (use the wrapper `./mvnw`)

## Prerequisites
- JDK 21 (LTS)
- Docker (for the local database)

## Getting started
```bash
# 1. Configure environment
cp .env.example .env        # then fill in real values (never committed)

# 2. Start the database
docker compose up -d db

# 3. Run the app (spring-boot:run activates the 'dev' profile via pom.xml)
./mvnw spring-boot:run

# 4. Run tests
./mvnw test
```

## Environment variables
Secrets come from `.env` (gitignored). See `.env.example` for the full list:

| Variable | Description |
|---|---|
| `DB_HOST` / `DB_PORT` | PostgreSQL host and port (`localhost` / `5432` for local dev) |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | PostgreSQL database and credentials |
| `DB_SSLMODE` | JDBC `sslmode` — `disable` for local dev; staging/prod default to `require` |
| `CVR_API_KEY` | Key for the CVR company-lookup API |
| `STRIPE_SECRET_KEY` | Stripe secret key (phase 4) |

## Profiles / environments
Select with `SPRING_PROFILES_ACTIVE`. There is intentionally **no default profile** in the
packaged jar — staging/prod must set it explicitly. `./mvnw spring-boot:run` activates `dev`
automatically (configured on the Spring Boot Maven plugin in `pom.xml`).

| Profile | Use | Schema handling |
|---|---|---|
| `dev` | local development (auto-activated by `spring-boot:run`) | `ddl-auto=update`, verbose SQL |
| `staging` | test environment | `ddl-auto=validate`, Flyway enabled |
| `prod` | production | `ddl-auto=validate`, Flyway enabled |

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
