# US-0011 — Migrate Database Stack to H2 — Technical Specification

## Overview

The Customer Portal currently requires an external PostgreSQL server for local development and integration testing. This story migrates the default runtime database to an in-memory H2 instance that runs in PostgreSQL compatibility mode (`MODE=PostgreSQL`). The migration is limited to build dependencies, Spring Boot datasource configuration, and the AI agent harness (`AGENTS.md` and the two `story-spec-*.SKILL.md` files). Liquibase remains the source of truth for schema management, and existing domain code is not changed.

## Scope Boundary

### In Scope

- **Dependency update in `pom.xml`:**
  - Remove the `org.postgresql:postgresql` runtime dependency.
  - Add `com.h2database:h2` as a `runtime` dependency.
- **Spring Boot datasource/JPA configuration in `application.yml`:**
  - H2 in-memory URL: `jdbc:h2:mem:customer_portal;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`.
  - Default username `sa` and empty password.
  - Driver class `org.h2.Driver`.
  - Hibernate dialect `org.hibernate.dialect.H2Dialect`.
  - H2 console enabled at `/h2-console`.
  - Keep Liquibase enabled and set `spring.jpa.hibernate.ddl-auto` to `none` because Liquibase is already in use.
- **AI harness alignment:**
  - Update `AGENTS.md` `### Database & Migrations` to list H2 (PostgreSQL compatibility mode) as the only supported database.
  - Update `story-spec-writer.SKILL.md` and `story-spec-reviewer.SKILL.md` so database/migration guidance points to `AGENTS.md` and no longer enforces PostgreSQL-only behavior.
- **Migration compatibility validation:**
  - Verify that existing Liquibase changelogs are valid for H2 in PostgreSQL mode.
  - Flag any PostgreSQL-only types or syntax (e.g., `JSONB`) that are not supported by the chosen H2 mode.

### Out of Scope

- Production deployment changes (the production datasource can still be supplied by environment variables at runtime).
- Rewriting existing business logic, controllers, services, repositories, or JPA entities.
- Replacing Liquibase with another migration tool or adding new schema changesets, unless an existing changeset is found to be incompatible.
- Removing `testcontainers:postgresql` from `pom.xml` (it is test-scoped and not a runtime driver; the decision is deferred).
- Front-end work, authentication changes, or new API endpoints.

## Actors & Use Cases

### Actors

- **Developer** — runs the application locally without a PostgreSQL installation.
- **CI Pipeline** — executes the build and integration test suite against the in-memory database.
- **AI Specification Agents** — use `AGENTS.md` and the skill files to generate/review H2-compatible specs.

### Use Cases

- **UC-1:** Developer starts the application locally and the H2 datasource is initialized automatically.
- **UC-2:** Integration tests run on an empty H2 in-memory database and Liquibase migrations execute successfully.
- **UC-3:** A new user story is written by `story-spec-writer` with persistence guidance that is valid for the configured H2 stack.
- **UC-4:** `story-spec-reviewer` verifies that a specification does not introduce PostgreSQL-only database features.

## API Contract

No public REST API endpoint is introduced by this story. The only web-facing surface is the H2 console, which is provided by Spring Boot and restricted to local development use.

- **H2 Console (dev-only):** `GET /h2-console`
  - Rendered by Spring Boot's H2 console servlet.
  - Not part of the Customer Portal API contract and must not be enabled in production profiles.

### Request / Response DTOs

Not applicable for this infrastructure migration.

### Error Responses

Runtime startup errors (for example, an invalid H2 URL or a Liquibase migration failure) are produced by Spring Boot's startup lifecycle and reported as application logs. They are not returned as RFC 7807 Problem Details because there is no controller involved.

## Business Rules

1. **BR-001 — Runtime driver replacement:** The only runtime JDBC dependency may be `com.h2database:h2`; the PostgreSQL driver must not be present in the runtime classpath.
2. **BR-002 — Default datasource URL:** The default `spring.datasource.url` must be `jdbc:h2:mem:customer_portal;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`. It can be overridden by the `SPRING_DATASOURCE_URL` environment variable.
3. **BR-003 — Default credentials:** The default username is `sa` and the default password is empty. Both can be overridden by `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.
4. **BR-004 — Dialect:** Hibernate/JPA must be configured with `org.hibernate.dialect.H2Dialect`.
5. **BR-005 — Schema management delegation:** Because Liquibase is configured, `spring.jpa.hibernate.ddl-auto` must be `none`. In a local/in-memory environment where Liquibase is intentionally disabled, `ddl-auto=update` is the acceptable fallback.
6. **BR-006 — H2 console:** The H2 console must be enabled at `/h2-console` for local development and disabled for any production-facing profile.
7. **BR-007 — Liquibase compatibility:** All existing Liquibase changesets must remain valid when executed against H2 in PostgreSQL compatibility mode. PostgreSQL-only constructs (e.g., `JSONB`, native procedural SQL) are not permitted unless confirmed to work in the configured H2 mode.
8. **BR-008 — AI harness consolidation:** `AGENTS.md` is the single source of truth for the database stack. `story-spec-writer.SKILL.md` and `story-spec-reviewer.SKILL.md` may refer to `AGENTS.md` but must not duplicate database-specific details.

## Data Model

No new JPA entity or persistence object is introduced by this story. Existing entities continue to map to tables managed by Liquibase.

### Existing Schema Validation

- All existing Liquibase changesets in `src/main/resources/db/changelog/changes/` must be loaded into H2 (PostgreSQL mode) without errors.
- If a changeset references a PostgreSQL-native type or function, it must be rewritten to a portable equivalent or guarded by a precondition that is valid in H2.

### Required Migration Changes

- No new changeset is required for the H2 migration itself, because the migration is configuration-only.
- A follow-up changeset is required **only** if an existing column uses a type (e.g., `JSONB`) that is not supported by H2 PostgreSQL mode.

## Security Considerations

- The H2 console is intended for local development only and must be disabled in production by a profile-specific override (`spring.h2.console.enabled=false`).
- The local H2 URL, username, and password (`sa`/empty) are safe for in-memory development; they are not shipped as secrets.
- No JWT, password hash, or user credential is logged or returned in the migration scope.
- `AGENTS.md` and the skill files must not expose real connection strings, credentials, tokens, or internal identifiers.

## Test Strategy

### Gherkin BDD Acceptance Criteria

```gherkin
Feature: Database stack migration to H2

  Scenario: AC-1 PostgreSQL driver is removed and H2 dependency is added
    Given the project build file `pom.xml`
    When the runtime dependencies are inspected
    Then the `org.postgresql:postgresql` runtime dependency is not present
    And the `com.h2database:h2` runtime dependency is present

  Scenario: AC-2 application.yml defaults to H2 in-memory database
    Given the default Spring profile and no external environment overrides
    When the application configuration is loaded
    Then the datasource URL is `jdbc:h2:mem:customer_portal;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`
    And the datasource username is `sa`
    And the datasource password is empty
    And the datasource driver class is `org.h2.Driver`

  Scenario: AC-3 Hibernate and H2 console are correctly configured
    Given the default Spring profile
    When the JPA and H2 console properties are inspected
    Then the Hibernate dialect is `org.hibernate.dialect.H2Dialect`
    And the H2 console is enabled at path `/h2-console`

  Scenario: AC-4 Schema management remains delegated to Liquibase
    Given Liquibase is configured in `application.yml`
    When the JPA properties are inspected
    Then `spring.liquibase.enabled` is `true`
    And `spring.jpa.hibernate.ddl-auto` is `none`

  Scenario: AC-5 AGENTS.md declares H2 as the supported database
    Given `AGENTS.md` is opened to `### Database & Migrations`
    When the database stack instructions are read
    Then H2 is listed as the only supported database
    And PostgreSQL compatibility mode (`MODE=PostgreSQL`) is documented
    And Liquibase is documented as the migration tool
    And `ddl-auto=update` is documented as the local/in-memory fallback when Liquibase is not used

  Scenario: AC-6 AI skill files defer database rules to AGENTS.md
    Given `story-spec-writer.SKILL.md` and `story-spec-reviewer.SKILL.md`
    When the prompt templates and constraints are reviewed
    Then no PostgreSQL-only database requirements remain
    And database/migration guidance points to `AGENTS.md` `### Database & Migrations`
    And the reviewer flags PostgreSQL-only constructs such as `JSONB` as incompatible

  Scenario: AC-7 Application starts and Liquibase migrations run against H2
    Given a clean in-memory H2 database
    When the Spring Boot application context starts
    Then Liquibase changelogs execute without errors
    And the application logs confirm a successful datasource initialization
    And the `DATABASECHANGELOG` table exists in H2

  Scenario: AC-8 Existing changelogs are compatible with H2 PostgreSQL mode
    Given the existing Liquibase changesets
    When they are executed against H2 in `MODE=PostgreSQL`
    Then all changesets apply successfully
    And no PostgreSQL-only types or syntax cause a failure
```

### Unit Tests

No new unit tests are required for this infrastructure story because there is no new service behavior. Existing service unit tests (with mocked repositories) should continue to pass after the dependency and configuration changes.

### API Tests

Not applicable. No controller or HTTP endpoint is introduced or modified.

### Integration Tests

- `shouldStartContext_WhenH2DatasourceIsConfigured` — starts the full Spring context and verifies that Liquibase runs successfully.
- `shouldUseH2Dialect_WhenApplicationStarts` — inspects the configured Hibernate dialect and asserts it is `H2Dialect`.
- `shouldEnableH2Console_WhenProfileIsDefault` — issues a request to `/h2-console` and confirms the login frame is returned (optional, dev profile only).
- `shouldRunExistingMigrations_WhenUsingH2PostgreSQLMode` — executes the full Liquibase changelog against an in-memory H2 database and fails if any changeset is incompatible.
- `shouldNotContainPostgresqlDriver_WhenClasspathIsInspected` — verifies the runtime classpath does not contain the PostgreSQL driver.

## Dependencies

- `spring-boot-starter-data-jpa` (already present) provides JPA/Hibernate and HikariCP autoconfiguration.
- `liquibase-core` (already present) for schema migrations.
- `com.h2database:h2` — new runtime dependency.
- `org.postgresql:postgresql` — to be removed.
- `AGENTS.md` and the two `story-spec-*.SKILL.md` files — to be updated as part of the same story.

## Open Questions / Assumptions

- Existing Liquibase changelogs are assumed to be compatible with H2 in PostgreSQL mode. If `JSONB` or other PostgreSQL-specific constructs are found, a separate database-agnostic changeset will be required.
- `testcontainers:postgresql` remains as a test-scoped dependency; whether to remove it is deferred.
- Production deployment is assumed to override datasource properties via environment variables. This story changes the local/CI defaults only.
- The H2 console is enabled by default for local development and is assumed to be disabled by a production profile or runtime override when needed.
