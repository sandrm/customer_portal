# US-001 — Register User

## User Story

As a Visitor  
I want to register using email and password  
so that I can create an account and access the Customer Portal.

## Acceptance Criteria

- **AC-1:** Valid registration returns HTTP 201 Created.
- **AC-2:** Duplicate email returns HTTP 409 Conflict.
- **AC-3:** Invalid email returns HTTP 400 Bad Request.
- **AC-4:** Passwords must satisfy password policy.
- **AC-5:** Response must never contain password or password hash.
