# US-0011 — Migrate Database Stack to H2

## User Story

As a development and testing team,  
I want the Customer Portal to run against an in-memory H2 database in PostgreSQL compatibility mode,  
so that the project no longer requires a PostgreSQL server for local development, CI, or lightweight testing.

## Description

a) DEPENDENCY UPDATE:
   - Inspect `pom.xml`. Remove or comment out the PostgreSQL driver dependency.
   - Add the official H2 database runtime dependency: `com.h2database:h2`.

b) APPLICATION CONFIGURATION:
   - Locate and modify the database configuration file (e.g., `application.properties` or `application.yml`).
   - Replace PostgreSQL properties with H2 configuration. Use an In-Memory URL with PostgreSQL compatibility mode:
     `jdbc:h2:mem:customer_portal;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`
   - Set username to `sa` and password to empty.
   - Enable the H2 console at `/h2-console`.
   - Update Hibernate/JPA properties to use `H2Dialect` and ensure `ddl-auto` is set to `update` (unless Flyway/Liquibase is detected).

c) Please analyze the current setup of these files and then proceed.

## Acceptance Criteria

- **AC-1:** The PostgreSQL JDBC driver is removed from `pom.xml` and the H2 runtime dependency is added.
- **AC-2:** `application.yml` (or `application.properties`) is configured with the H2 in-memory URL, `sa`/empty credentials, H2 console at `/h2-console`, `H2Dialect`, and `ddl-auto` set to `update` unless a migration tool is already in use.
- **AC-3:** `AGENTS.md` is updated so the database stack is H2 in PostgreSQL compatibility mode.
- **AC-4:** `story-spec-writer.SKILL.md` and `story-spec-reviewer.SKILL.md` defer database/migration rules to `AGENTS.md` and no longer require PostgreSQL-only features.

## Notes

- If Liquibase is already configured, existing changelogs must remain valid for H2 in PostgreSQL compatibility mode. Any PostgreSQL-only syntax (e.g., `JSONB`) must be made database-agnostic.
- No new domain logic, endpoints, or persistence entities are in scope for this story.
