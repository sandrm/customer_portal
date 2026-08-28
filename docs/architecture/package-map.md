# Package Responsibility Map

This document describes the purpose and allowed contents of each source package in the Customer Portal backend.

## Root package

- `com.example.customerportal`
  - Application entry point and cross-domain configuration (e.g., security, global exception handling).

## Domain packages

Each business domain lives under `com.example.customerportal.<domain>` and contains the following packages:

| Package | Responsibility |
| --- | --- |
| `com.example.customerportal.<domain>.controller` | REST endpoints |
| `com.example.customerportal.<domain>.service` | Business logic |
| `com.example.customerportal.<domain>.repository` | Persistence |
| `com.example.customerportal.<domain>.entity` | Persistence model |
| `com.example.customerportal.<domain>.dto` | API contracts |

### Example domains

- `com.example.customerportal.auth` — registration, login, token refresh.
- `com.example.customerportal.profile` — user profile read/update.

## Layer rules

- Controllers depend only on services and DTOs.
- Services contain business logic and coordinate repositories.
- Repositories manage persistence for entities.
- Entities are never exposed through REST endpoints; DTOs are used as API contracts.
