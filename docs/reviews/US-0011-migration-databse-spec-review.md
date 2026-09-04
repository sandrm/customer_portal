# Review Report: US-0011 — Migrate Database Stack to H2

Review of [docs/specifications/US-0011-migration-databse-spec.md](../specifications/US-0011-migration-databse-spec.md) against [docs/backlog/US-0011-migration-databse.md](../backlog/US-0011-migration-databse.md) and [AGENTS.md](../AGENTS.md).

## 1. Summary

The specification is well-structured and captures the intent of the backlog: move the project from PostgreSQL to an in-memory H2 database with PostgreSQL compatibility mode, update the Maven dependencies, align `AGENTS.md` and the AI skill files, and keep Liquibase as the schema authority. It correctly expands the acceptance criteria into Gherkin BDD scenarios and defines the expected configuration. A few areas need clarification before the spec is approved: the hypothetical follow-up changeset must be tied to `AGENTS.md` atomicity/rollback rules, the H2 console security/test handling should be made explicit, and the leftover `testcontainers:postgresql` dependency should be resolved or justified.

> Implementation-status note: The current `pom.xml` and `application.yml` in the repository still contain PostgreSQL values. This is expected before the story is implemented, but the implementation will need to match the spec.

## 2. Coverage Check

All four backlog acceptance criteria are addressed:

- **AC-1** (PostgreSQL driver removed, H2 added) — covered in `Scope Boundary` → `In Scope`, `BR-001`, and Gherkin `AC-1`.
- **AC-2** (`application.yml` configured for H2, `sa`/empty password, H2 console, `H2Dialect`, `ddl-auto` handling) — covered in `Scope Boundary`, `BR-002` through `BR-006`, and Gherkin `AC-2`, `AC-3`, `AC-4`.
- **AC-3** (`AGENTS.md` updated to H2 in PostgreSQL compatibility mode) — covered in `BR-008` and Gherkin `AC-5`.
- **AC-4** (AI skill files defer to `AGENTS.md`) — covered in `BR-008` and Gherkin `AC-6`.

Backlog notes about Liquibase changelog compatibility and no new domain logic are also covered (`BR-007`, `Data Model` → `Existing Schema Validation`, and `Out of Scope`).

## 3. Architecture & Conventions Review

- **Layer ordering (Controller → Service → Repository):** Not applicable. No new controller, service, or repository is introduced.
- **DTOs/records for API payloads:** Not applicable. The `API Contract` section correctly states no new request/response DTO is introduced.
- **JPA entities not exposed in controllers:** Not applicable. No new entity is introduced.
- **Package and naming conventions:** Not applicable.

The spec stays within infrastructure concerns and does not invent new domain packages or entities.

## 4. API Contract Review

- **HTTP method/path:** `GET /h2-console` is listed as the only web-facing surface. This is a Spring Boot development servlet, not a public Customer Portal API endpoint. The spec makes that distinction, but it would be clearer to place the H2 console under a `Developer Tools` or `Operational Endpoints` subsection rather than the `API Contract` section.
- **Request/Response DTOs:** Correctly marked as `Not applicable`.
- **Error responses mapped to RFC 7807 Problem Details:** Not applicable. Startup errors (bad URL, Liquibase failure) are logged by Spring Boot. The spec notes they are not returned as Problem Details, which is acceptable because there is no controller or HTTP API in this story. If the H2 console request itself fails (e.g., 404/403), that is container-level and outside the application contract.

## 5. Data Model Review

- **JPA entities:** No new entity is introduced. Existing entities continue to map to Liquibase-managed tables. This is correct for a configuration-only migration.
- **Persistence changes / changesets:** The spec states that "No new changeset is required for the H2 migration itself" and that a follow-up changeset is required only if an existing column uses an unsupported type such as `JSONB`.
- **Database-specific constructs:** `BR-007` and Gherkin `AC-8` correctly flag PostgreSQL-only types/syntax (`JSONB`, native procedural SQL) that are not guaranteed to work in H2 PostgreSQL mode.

## 6. Security Review

- **Authentication/authorization for any new endpoint:** Not applicable. No new business endpoint is introduced.
- **Passwords/tokens:** No credentials are involved in this story. The local H2 `sa`/empty password is correctly described as an in-memory development default and not a shipped secret.
- **H2 console security:** The spec says the console must be "disabled for any production-facing profile." This is correct, but it does not state *how* (e.g., `application-prod.yml` or an environment override setting `spring.h2.console.enabled=false`). It also does not address how stateless JWT security (`AGENTS.md` §2.3) interacts with the unauthenticated `/h2-console` path in the default/dev profile.
- **Raw exception messages:** Not applicable for this story.

## 7. Test Strategy Review

- **Unit/API/Integration coverage:** Described. Unit tests are correctly marked as not requiring new behavior tests; integration tests cover context startup, dialect, migrations, and H2 console exposure.
- **MockMvc and security context:** The spec does not include API/MockMvc tests, which is correct. However, the integration test `shouldEnableH2Console_WhenProfileIsDefault` performs an HTTP request to `/h2-console` without describing the security context. With stateless JWT enabled, this test may fail unless security is configured to permit the H2 console path in the test/dev profile. This should be clarified.
- **Integration tests use the in-memory H2 setup:** Yes. `shouldStartContext_WhenH2DatasourceIsConfigured`, `shouldUseH2Dialect_WhenApplicationStarts`, `shouldRunExistingMigrations_WhenUsingH2PostgreSQLMode`, and `shouldNotContainPostgresqlDriver_WhenClasspathIsInspected` all align with `AGENTS.md` `### Database & Migrations`.
- **Gherkin acceptance criteria:** Present and in Gherkin syntax. Scenarios `AC-1` through `AC-6` are largely static configuration/file checks rather than runtime behavior scenarios. This is acceptable for an infrastructure migration, but the readability would improve if some were rewritten as runtime behavior (e.g., "When the application context loads").

## 8. Constraint Compliance

- **No implementation code / controller/service/repository classes / migration script snippets:** The spec contains only prose and Gherkin examples. No Java classes, Liquibase XML/SQL snippets, or implementation code is present. ✓
- **No speculative domain logic:** Scope is tightly limited to dependency/config/AI-harness changes. ✓
- **AGENTS.md compliance:** References `AGENTS.md` `### Database & Migrations` for the database stack and atomic-changeset rules. ✓

## 9. Issues Found

1. **[blocking] Data Model — follow-up changeset rules not tied to `AGENTS.md`**
   - **Location:** `Data Model` → `Required Migration Changes`
   - **Quote:** "A follow-up changeset is required **only** if an existing column uses a type (e.g., `JSONB`) that is not supported by H2 PostgreSQL mode."
   - **Violation:** `AGENTS.md` `### Database & Migrations` requires every changeset to be atomic, stored in a timestamped file, and accompanied by a valid rollback. The spec mentions a possible follow-up changeset but does not state that it must obey these rules.
   - **Recommendation:** Add a sentence such as: "Any follow-up changeset must be atomic, stored in `src/main/resources/db/changelog/changes/YYYYMMDD_HHMMSS__*.xml` or `.sql`, and include a valid `<rollback>` or `rollbackSQL`."

2. **[suggestion] H2 console security/test context is underspecified**
   - **Location:** `Security Considerations` and `Test Strategy` → `Integration Tests` → `shouldEnableH2Console_WhenProfileIsDefault`
   - **Concern:** `AGENTS.md` `### Spring Security 6.x (Mandatory)` requires stateless JWT authentication and no bypass. The H2 console endpoint is unauthenticated by default. Without explicit security configuration (permitting `/h2-console` in dev/test profiles), the `shouldEnableH2Console_WhenProfileIsDefault` test is likely to fail or require disabling security, which would violate `AGENTS.md` §4 Testing Requirements.
   - **Recommendation:** Either describe the security rule that permits `/h2-console` for dev/test, or move the H2-console HTTP assertion to a profile that explicitly relaxes security for that path, and document it in `Security Considerations`.

3. **[suggestion] `testcontainers:postgresql` should be removed or justified**
   - **Location:** `Scope Boundary` → `Out of Scope`
   - **Quote:** "Removing `testcontainers:postgresql` from `pom.xml` (it is test-scoped and not a runtime driver; the decision is deferred)."
   - **Concern:** `AGENTS.md` `### Database & Migrations` states H2 is the only supported database and integration tests must use the in-memory H2 setup. Keeping a PostgreSQL Testcontainers dependency is inconsistent with the H2-only posture and may confuse future maintainers.
   - **Recommendation:** Either remove the dependency as part of this story or explicitly document that it remains for a non-default integration-test profile that validates against real PostgreSQL (if such a profile is intentionally kept).

4. **[suggestion] Gherkin scenarios are mostly static file checks**
   - **Location:** `Test Strategy` → `Gherkin BDD Acceptance Criteria`, scenarios `AC-1` through `AC-6`
   - **Concern:** BDD scenarios that inspect `pom.xml`, `application.yml`, or `AGENTS.md` as static files are less readable than behavior-driven scenarios.
   - **Recommendation:** Rewrite the most important scenarios to describe runtime behavior, e.g.:
     - `Given the application starts with no SPRING_DATASOURCE_URL override`
     - `When the datasource is autoconfigured`
     - `Then the URL is jdbc:h2:mem:customer_portal;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`

5. **[suggestion] Unit Tests section contradicts `AGENTS.md` DoD wording**
   - **Location:** `Test Strategy` → `Unit Tests`
   - **Quote:** "No new unit tests are required for this infrastructure story because there is no new service behavior."
   - **Concern:** `AGENTS.md` §5 requires "New or updated unit, API, and integration tests exist and pass." The claim that no unit tests are required may be interpreted as no unit-test work at all, while existing tests may need updating if configuration assumptions changed.
   - **Recommendation:** Rephrase to: "Existing unit tests continue to pass without change; new unit tests are added only if configuration-specific behavior is introduced."

## 10. Action Items

1. **(Highest)** Update `Data Model` → `Required Migration Changes` to require that any follow-up changeset follow `AGENTS.md` atomicity, timestamped-file, and rollback rules.
2. Clarify H2-console security handling in `Security Considerations` and the `shouldEnableH2Console_WhenProfileIsDefault` test (or remove that test if security cannot be relaxed).
3. Decide on `testcontainers:postgresql` — remove it or document the reason it remains, because `AGENTS.md` treats H2 as the only supported database.
4. Optionally rewrite Gherkin `AC-1`–`AC-6` to be more behavior-oriented.
5. Rephrase the `Unit Tests` paragraph to acknowledge that existing unit tests may need updating and that new tests are added only if needed.

---

**Verdict:** Approve after addressing **Issue 1**; the other items are suggestions that improve clarity and consistency with `AGENTS.md`.
