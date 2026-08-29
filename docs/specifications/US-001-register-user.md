# US-001 — Register User — Technical Specification

## Overview

The Customer Portal needs a public, unauthenticated endpoint that allows a Visitor to create an account by providing a unique email address and a password that satisfies the platform's password policy. On success, the backend persists a new user record and returns a sanitized representation of the created account.

## Scope Boundary

### In Scope

- Public HTTP endpoint for account creation.
- Request validation for email format and password policy.
- Enforcement of email uniqueness.
- Secure password hashing before persistence.
- Returning a 201 Created response that never includes the password or its hash.
- RFC 7807 Problem Details for error responses.

### Out of Scope

- Email verification / confirmation tokens.
- User roles, permissions, or profile management.
- Authentication session or token issuance after registration.
- Rate limiting, CAPTCHA, or abuse prevention.
- Front-end implementation.

## Actors & Use Cases

- **Visitor** — an unauthenticated user who wants to create an account.

### Use Cases

- UC-1: Visitor submits a valid email and password and receives a new account.
- UC-2: Visitor submits an already-registered email and is informed of the conflict.
- UC-3: Visitor submits an invalid email or weak password and is informed of the validation error.

## API Contract

### Endpoint

```
POST /api/v1/auth/register
```

### Request DTO: `RegistrationRequest`

A Java `record` with the following fields:

- `email`: `String` (required)
  - Must be present and not blank.
  - Must conform to RFC 5322 email format.
  - Leading/trailing whitespace is trimmed and the value is normalized to lowercase before validation and persistence.
- `password`: `String` (required)
  - Must be present and not blank.
  - Must satisfy the password policy defined in Business Rule BR-002.

```java
public record RegistrationRequest(
    String email,
    String password
) {}
```

### Successful Response: 201 Created

Returns a `RegisteredUserResponse` `record`:

- `id`: `UUID` — unique identifier of the created user.
- `email`: `String` — the registered, normalized email address.
- `createdAt`: `OffsetDateTime` — UTC timestamp of account creation.

```java
public record RegisteredUserResponse(
    UUID id,
    String email,
    OffsetDateTime createdAt
) {}
```

Response headers:

- `Content-Type: application/json`
- `Location: /api/v1/users/{id}` (optional, if a user-read endpoint exists)

### Error Responses

All errors are returned as `application/problem+json` following RFC 7807.

#### 400 Bad Request — Validation errors

Invalid email format or password does not satisfy the policy.

```json
{
  "type": "https://customerportal.example/errors/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "The provided registration data is invalid.",
  "invalidParams": [
    { "field": "email", "reason": "must be a valid email address" },
    { "field": "password", "reason": "must be at least 8 characters long" }
  ]
}
```

#### 409 Conflict — Duplicate email

The email is already in use.

```json
{
  "type": "https://customerportal.example/errors/email-already-registered",
  "title": "Email already registered",
  "status": 409,
  "detail": "An account with the given email address already exists."
}
```

## Business Rules

1. **BR-001 — Email required and valid**: A registration request must contain an email that is non-blank and matches a valid email format. The value is normalized to lowercase and trimmed before persistence.
2. **BR-002 — Password policy**: The password must be at least 8 characters, contain at least one uppercase letter, one lowercase letter, one digit, and one special character. Maximum length is 128 characters.
3. **BR-003 — Email uniqueness**: The email must not already exist in the `users` table. The check must be case-insensitive on the normalized email.
4. **BR-004 — Password hashing**: The raw password is hashed using BCrypt with `BCryptPasswordEncoder` before storage. The hashed value is never returned.
5. **BR-005 — Sanitized response**: The response body must not contain `password`, `passwordHash`, or any form of the secret.
6. **BR-006 — Atomic persistence**: The user record is persisted in a single atomic transaction. If any constraint violation occurs, the transaction rolls back and an appropriate Problem Detail is returned.

## Data Model

### Entity: `User`

A JPA entity mapped to the `users` table:

- `id`: `UUID` — primary key, generated.
- `email`: `String` — unique, not null, stored in lowercase.
- `passwordHash`: `String` — not null, stores the BCrypt hash.
- `createdAt`: `OffsetDateTime` — not null, set on creation.
- `updatedAt`: `OffsetDateTime` — updated on any mutation.

Constraints:

- Unique constraint on `email`.
- `password_hash` length must accommodate BCrypt output (minimum 60 characters).

### Liquibase Changeset

A single atomic changeset (timestamped file in `src/main/resources/db/changelog/changes/`) should:

1. Create the `users` table with columns `id`, `email`, `password_hash`, `created_at`, `updated_at`.
2. Add a primary key on `id`.
3. Add a unique constraint/index on `email`.
4. Include a valid rollback that drops the `users` table.

No auto-generation (`hibernate.hbm2ddl.auto`) may be used.

## Security Considerations

- The endpoint must be **public and stateless**; no JWT is required to register.
- The raw `password` field must be cleared from memory as soon as possible and never logged, not even in error messages.
- The `passwordHash` is sensitive and must never be serialized into any response.
- Use `BCryptPasswordEncoder` for hashing.
- Stack traces, SQL, and internal identifiers must not be returned in Problem Details.

## Test Strategy

### Gherkin BDD Acceptance Criteria

```gherkin
Feature: User Registration

  Scenario: AC-1 Valid registration returns HTTP 201 Created
    Given a Visitor provides a valid email "new.user@example.com" and a strong password
    When the Visitor submits the registration request
    Then the API responds with 201 Created
    And the response body contains a unique user id and the registered email
    And the response body does not contain the password or password hash

  Scenario: AC-2 Duplicate email returns HTTP 409 Conflict
    Given an account with email "existing.user@example.com" already exists
    When the Visitor submits a registration request with email "existing.user@example.com" and a strong password
    Then the API responds with 409 Conflict
    And the response body is an RFC 7807 Problem Detail with title "Email already registered"

  Scenario: AC-3 Invalid email returns HTTP 400 Bad Request
    Given a Visitor provides an invalid email "not-an-email"
    And a strong password
    When the Visitor submits the registration request
    Then the API responds with 400 Bad Request
    And the response body is an RFC 7807 Problem Detail
    And the validation errors include the "email" field

  Scenario: AC-4 Password must satisfy password policy
    Given a Visitor provides a valid email "user@example.com"
    And a password "123" that does not satisfy the password policy
    When the Visitor submits the registration request
    Then the API responds with 400 Bad Request
    And the response body is an RFC 7807 Problem Detail
    And the validation errors include the "password" field

  Scenario: AC-5 Response must never contain password or password hash
    Given a Visitor provides a valid email "secure.user@example.com" and a strong password
    When the Visitor submits the registration request
    Then the API responds with 201 Created
    And the JSON response contains neither "password" nor "passwordHash"
```

### Unit Tests

Example method names:

- `shouldReturnRegisteredUser_WhenRegistrationDataIsValid`
- `shouldThrowDuplicateEmailException_WhenEmailAlreadyExists`
- `shouldThrowValidationException_WhenPasswordTooShort`
- `shouldHashPassword_WhenRegisteringNewUser`

### API Tests

- `shouldReturn201_WhenRegisteringWithValidData`
- `shouldReturn400_WhenEmailIsInvalid`
- `shouldReturn409_WhenEmailIsDuplicate`
- `shouldReturn400_WhenPasswordIsWeak`
- `shouldNotExposePasswordOrHash_WhenRegistrationSucceeds`

### Integration Tests

- Liquibase migration runs successfully against a real PostgreSQL container.
- Unique constraint on `email` is enforced at the database level.
- End-to-end registration persists a user and returns a sanitized response.

## Dependencies

- Global exception handling and Problem Detail response setup (must exist).
- `users` table and Liquibase migration (new, defined in this story).
- `BCryptPasswordEncoder` bean from `spring-security-crypto`.
- Email format validation utility (e.g. `jakarta.validation.constraints.Email` or custom validator).

## Open Questions / Assumptions

- The exact password policy (length, special character count) is assumed as defined in BR-002 and must be confirmed by product/security.
- No `confirmPassword` field is required; if a front-end asks for confirmation, that is a client-side concern only.
- Email verification is out of scope and will be handled by a later story.
- A default role is not assigned; role management will be covered by a later story.
- The `Location` header in the 201 response is optional and depends on the user-read endpoint being implemented separately.
