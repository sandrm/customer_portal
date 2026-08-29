# Customer Portal — AGENTS.md

Persistent project-level instructions for the Customer Portal backend.

---

## 1. Project & Scope

- **Name:** Customer Portal (Backend application)
- **Description:** A backend service that enables users to **register**, **authenticate**, and **manage profiles**.
- **Delivery scope:** Backend Java/Spring Boot service only. Do not add frontend code, separate modules, or speculative domain logic unless explicitly requested.
- Design new code as isolated, testable components that can be extended without rewriting existing layers.

---

## 2. Tech Stack & Versions

### Java 21 (Mandatory)

- Use `record` for Data Transfer Objects (DTOs) and API responses.
- Use pattern matching for `instanceof` and `switch` expressions.
- Use text blocks for multi-line strings, SQL snippets, and JSON log payloads.

### Spring Boot 3.x (Mandatory)

- Always use modern, type-safe configuration (YAML or `@ConfigurationProperties`).
- Implement global exception handling through a single `@ControllerAdvice`.
- Return **RFC 7807 Problem Details** (`application/problem+json`) for every error response.
- Do not use legacy `web.xml`, `@EnableWebMvc`, or manual `MessageSource` configurations unless absolutely required.

### Spring Security 6.x (Mandatory)

- Authentication must be **stateless** and use **JWT access and refresh tokens**.
- Do not enable session management or rely on server-side HTTP sessions.
- Implementation guidance is in `Skills.md` under `Implement-Stateless-JWT-Auth`.

### Database & Migrations

- **PostgreSQL** is the only supported database.
- **Liquibase** is the only authorized migration tool.
- Every changeset must be:
  - Atomic (one logical change per changeset).
  - Stored in a timestamped file (e.g., `db/changelog/changes/YYYYMMDD_HHMMSS__description.xml` or `.sql`).
  - Accompanied by a valid `<rollback>` script or `rollbackSQL`.
- Do not use `hibernate.hbm2ddl.auto=update`, `spring.jpa.hibernate.ddl-auto=update`, or any other schema auto-generation.

### Lombok

- Allowed on any class: `@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Builder`, and `@Slf4j`.
- **Strictly prohibited on JPA Entities:** `@Data`, `@ToString`, and `@EqualsAndHashCode`.
- Do not use Lombok to generate `toString()` on entities that may expose lazy associations.

---

## 3. Architecture & Conventions

### Clean Layered Architecture

- Always maintain the strict layer order: **Controller → Service → Repository**.
- **Entities must never leak into Controllers.** Use DTOs/Records for all API request and response payloads.
- Controllers must be thin; business rules belong in Services.
- Services must not directly execute SQL or manage transactions beyond their own boundary.

### State & Naming

- The application is **stateless**. Do not rely on server-side HTTP sessions or in-memory state.
- Follow standard Java naming conventions:
  - `PascalCase` for classes and enums.
  - `camelCase` for methods, fields, and local variables.
  - `UPPER_SNAKE_CASE` for constants.
- Packages must be domain-driven (e.g., `com.example.customerportal.auth`, `...profile`) and not technology-driven.

### Error Handling

- Represent business errors with custom exceptions that map to precise HTTP status codes.
- The global `@ControllerAdvice` must translate exceptions into Problem Details responses.
- Do not expose stack traces, SQL, or internal identifiers in API error bodies.

### Logging & Security

- Always use SLF4J through Lombok `@Slf4j`.
- **Do not use `System.out.println` or `System.err.println`.**
- **Never log passwords, JWT access tokens, refresh tokens, secrets, or unmasked PII.**
- Always hash passwords with `BCryptPasswordEncoder` before storage.
- Do not return raw exception messages to clients; log the detail and return a sanitized Problem Detail.

---

## 4. Testing Requirements

### Unit Tests

- Use **JUnit 5** and **Mockito** for Service-layer tests.
- Name every test method using the format: `should[Expectation]_When[Condition]`.
- Use **AssertJ** for all assertions.
- Do not write tests that assert on implementation details; assert on observable behavior.

### API Tests

- Use **MockMvc** for controller-level tests.
- Mock the security context with `@WithMockUser` or a custom JWT test annotation.
- **Do not disable or bypass Spring Security in tests.**
- Assert on the full Problem Details response for error cases.

### Integration Tests

- Use **Testcontainers** with a real PostgreSQL instance.
- Use Spring Boot 3.x native **`@ServiceConnection`** to wire the container.
- **Do not use the legacy `@DynamicPropertySource` pattern.**
- Do not use an embedded H2 database for any test.
- Always assert that Liquibase migrations run successfully in the test context.

---

## 5. Definition of Done (DoD)

A task is considered complete only when **all** the following conditions are satisfied:

- [ ] The project compiles cleanly: `mvn clean compile` passes with no errors or warnings.
- [ ] A Liquibase migration script has been added or updated, is atomic, timestamped, and includes a rollback. Existing committed changesets are not modified.
- [ ] Source code is formatted consistently with the project style.
- [ ] New or updated unit, API, and integration tests exist and pass.
- [ ] No JPA entity uses `@Data`, `@ToString`, or `@EqualsAndHashCode`.
- [ ] JPA entities are not leaked into controllers or returned as JSON; DTOs/records are used.
- [ ] No method returns `null` where `java.util.Optional` is the expected contract.
- [ ] No `hibernate.hbm2ddl.auto=update` or `spring.jpa.hibernate.ddl-auto=update` is present.
- [ ] No embedded H2 database is used for testing or local development.
- [ ] No `System.out.println` or `System.err.println` calls remain in the changed code.
- [ ] No passwords, tokens, or secrets are logged or exposed in responses.
- [ ] Spring Security is not disabled, bypassed, or ignored in tests; the security context is mocked.
- [ ] No speculative domain logic, modules, or tables are added unless explicitly requested.

---

## 6. Repository Structure

- Source packages are domain-driven under `com.example.customerportal.<domain>`:
  - Web layer: `com.example.customerportal.<domain>.web`
  - Service layer: `com.example.customerportal.<domain>.service`
  - Persistence layer: `com.example.customerportal.<domain>.persistence`
  - DTOs/records: `com.example.customerportal.<domain>.dto`
  - Exceptions: `com.example.customerportal.<domain>.exception`
- Liquibase changelogs: `src/main/resources/db/changelog/changes/`
- Tests mirror the `src/main/java` package structure under `src/test/java`

## 7. References

- [Product Vision](docs/product/product-vision.md)
- [Epic Map](docs/product/epic-map.md)
