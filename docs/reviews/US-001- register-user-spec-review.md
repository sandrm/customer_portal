
# Review Report: US-001 — Register User Specification

## Scope Note

I found no implementation source files in the repository, so this review covers [docs/specifications/US-001-register-user.md](cci:7://file:///D:/projects_2026/customer_portal/docs/specifications/US-001-register-user.md:0:0-0:0) against [docs/backlog/US-001-register-user.md](cci:7://file:///D:/projects_2026/customer_portal/docs/backlog/US-001-register-user.md:0:0-0:0), [AGENTS.md](cci:7://file:///D:/projects_2026/customer_portal/AGENTS.md:0:0-0:0), and [docs/architecture/package-map.md](cci:7://file:///D:/projects_2026/customer_portal/docs/architecture/package-map.md:0:0-0:0).

## 1. What Meets the Criteria Perfectly

- **[Story coverage]**: All five acceptance criteria (AC-1 through AC-5) are explicitly addressed.
- **[API-first design]**: `RegistrationRequest` and `RegisteredUserResponse` are defined as Java `record`s before the data model is introduced.
- **[Scope discipline]**: In/out of scope lists are clear and exclude speculative features (email verification, roles, token issuance, front-end).
- **[Error mapping]**: 400 validation and 409 duplicate email are mapped to RFC 7807 Problem Details.
- **[Security basics]**: Passwords are BCrypt-hashed, never logged, and never returned.
- **[Data model]**: `users` table, unique `email`, and `password_hash` length are all present.

## 2. Critical Issues / Lecturer-Flag Risks

- **[C-1] One Liquibase changeset does multiple logical changes** — [AGENTS.md](cci:7://file:///D:/projects_2026/customer_portal/AGENTS.md:0:0-0:0) §2 "Liquibase" requires "one logical change per changeset." The Data Model proposes a single changeset that creates the table, adds the primary key, **and** adds a unique index/constraint. This is likely to be flagged.
- **[C-2] `invalidParams` is not the standard RFC 7807 extension name** — The 400 Problem Detail example uses `invalidParams`. The conventional Spring/RFC 7807 extension is `invalid-params` (kebab-case). A strict grader may mark this as non-compliant.
- **[C-3] "Clear raw password from memory" is unrealistic with `String`** — Java `String` is immutable and cannot be securely cleared. The spec should not claim this while using a `String` DTO.
- **[C-4] Test strategy is missing mandatory tooling details** — [AGENTS.md](cci:7://file:///D:/projects_2026/customer_portal/AGENTS.md:0:0-0:0) §4 requires explicit JUnit 5, Mockito, AssertJ, MockMvc, and the configured in-memory H2 database (PostgreSQL compatibility mode) with Liquibase migration assertions. The spec lists Gherkin and example method names but does not name the required frameworks.
- **[C-5] JPA entity Lombok prohibitions are missing** — [AGENTS.md](cci:7://file:///D:/projects_2026/customer_portal/AGENTS.md:0:0-0:0) §2.5 forbids `@Data`, `@ToString`, and `@EqualsAndHashCode` on entities. The `User` description does not state this.
- **[C-6] `Location` header is speculative** — `Location: /api/v1/users/{id}` references an endpoint that does not exist and is out of scope. Strict scope checks may flag this.
- **[C-7] Validation annotations are not described** — The `RegistrationRequest` `record` is shown with no validation hints (`@NotBlank`, `@Email`, `@Pattern`, etc.). Since this is the API contract, the validation approach should be stated.

## 3. Suggested Fixes

- **[Fix C-1]**: Split Liquibase into three atomic changesets (each with its own rollback): create `users` table; add primary key constraint; add unique `email` constraint/index.
- **[Fix C-2]**: Change `invalidParams` to `invalid-params` in the 400 Problem Detail example.
- **[Fix C-3]**: Remove the "cleared from memory" claim, or rephrase to: "The raw password is not retained, logged, or returned; it is hashed immediately and left for GC." Alternatively, switch the DTO to `char[]` and describe `Arrays.fill` after hashing.
- **[Fix C-4]**: Add a "Testing Tooling" subsection explicitly naming JUnit 5, Mockito, AssertJ, MockMvc, and the configured in-memory H2 database (PostgreSQL compatibility mode) with Liquibase migration assertions.
- **[Fix C-5]**: Add a line to the `User` entity description: "Allowed Lombok: `@Getter`/`@Setter`; prohibited: `@Data`, `@ToString`, `@EqualsAndHashCode`."
- **[Fix C-6]**: Remove the optional `Location` header; return only `Content-Type: application/json` for the 201 response.
- **[Fix C-7]**: Mention validation annotations on the `RegistrationRequest` record, e.g., `@NotBlank`, `@jakarta.validation.constraints.Email`, and a custom `@StrongPassword` or `@Pattern` validator.

## Verdict

The specification is well-structured and implementation-ready in concept, but **C-1 (Liquibase atomicity)** and **C-2 (`invalidParams` naming)** are the highest-risk items for an [AGENTS.md](cci:7://file:///D:/projects_2026/customer_portal/AGENTS.md:0:0-0:0)-driven grader. Fix those first before submission.