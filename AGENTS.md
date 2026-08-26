# AGENTS.md — Customer Portal System Instructions

These are the persistent, mandatory instructions for every AI agent working on the **Customer Portal** backend. All code, configuration, migrations, and tests must conform to the rules below. When in doubt, apply the strictest possible interpretation.

---

## 1. Project & Scope

- **Name:** Customer Portal (Backend application)
- **Description:** A backend service that enables users to **register**, **authenticate**, and **manage profiles**. The system is designed as a modular foundation for future features such as product catalog, ordering, and support.
- **Delivery scope:** Backend Java/Spring Boot service only. Do not add frontend code, separate modules, or speculative future domain logic unless explicitly requested.
- Always design new code as an isolated, testable component that can be extended without rewriting existing layers.

---

## 2. Tech Stack & Versions

### Java 21 (Mandatory)

- Always use `record` for Data Transfer Objects (DTOs) and API responses.
- Always prefer pattern matching for `instanceof` and `switch` expressions.
- Always use text blocks for multi-line strings, SQL snippets, and JSON log payloads.
- Do not use pre-Java 16 idioms where a modern language feature is applicable.

### Spring Boot 3.x (Mandatory)

- Always use modern, type-safe configuration (YAML or `@ConfigurationProperties`).
- Implement global exception handling through a single `@ControllerAdvice`.
- Return **RFC 7807 Problem Details** (`application/problem+json`) for every error response.
- Do not use legacy `web.xml`, `@EnableWebMvc`, or manual `MessageSource` configurations unless absolutely required.

### Spring Security 6.x (Mandatory)

- Implement **stateless JWT authentication**.
- Issue and validate separate **Access** and **Refresh** tokens.
- Implement custom `OncePerRequestFilter` for token extraction and validation.
- Provide a dedicated `AuthenticationEntryPoint` and `AccessDeniedHandler`.
- Do not enable session management; the server is stateless.

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
- [ ] A Liquibase migration script has been added or updated, is atomic, timestamped, and includes a rollback.
- [ ] Source code is formatted consistently with the project style.
- [ ] New or updated unit, API, and integration tests exist and pass.
- [ ] No JPA entity uses `@Data`, `@ToString`, or `@EqualsAndHashCode`.
- [ ] No method returns `null` where `java.util.Optional` is the expected contract.
- [ ] No `hibernate.hbm2ddl.auto=update` or `spring.jpa.hibernate.ddl-auto=update` is present.
- [ ] No embedded H2 database is used for testing or local development.
- [ ] No `System.out.println` or `System.err.println` calls remain in the changed code.
- [ ] No passwords, tokens, or secrets are logged or exposed in responses.

---

## 6. Prohibited Actions

Do **not** perform any of the following under any circumstance:

- Do not apply `@Data`, `@ToString`, or `@EqualsAndHashCode` to JPA Entities.
- Do not return `null` from repository or service methods that can return `Optional<T>`.
- Do not use `hibernate.hbm2ddl.auto=update` or `spring.jpa.hibernate.ddl-auto: update`.
- Do not use an embedded H2 database for tests, local development, or demonstrations.
- Do not disable, bypass, or ignore Spring Security in tests; always mock the authentication context.
- Do not leak JPA Entities into Controllers or expose them as JSON responses; use DTOs/Records.
- Do not log passwords, JWT tokens, refresh tokens, API keys, or other secrets.
- Do not use `System.out.println` or `System.err.println` for logging.
- Do not modify existing Liquibase changesets that have already been committed; always add a new changeset.
- Do not create speculative future modules or tables for product catalog, ordering, or support unless explicitly requested.

---

## 7. General Agent Behavior

- Always re-read this file at the start of a session before proposing changes.
- Always prefer minimal, focused edits over large refactors.
- Always justify new dependencies before adding them.
- If a requirement conflicts with these instructions, state the conflict explicitly and ask for clarification rather than assuming an override.
