# Epic Map

## EPIC 1 — USERS
- [ ] **Register User** — Account creation flow, initial profile database entry.
- [ ] **Verify Email** — Security confirmation tokens, post-registration onboarding triggers.
- [ ] **Update Profile** — Editing user metadata, avatar uploads, and preference settings.
- [ ] **Deactivate Account** — Soft-deletion policies, data retention, and access revocation.

## EPIC 2 — AUTHENTICATION
- [ ] **Login** — Credentials verification, OAuth2/JWT issuance, session handling.
- [ ] **Logout** — Token blacklisting, clearing active sessions, client-side state cleanup.
- [ ] **Refresh Token** — Silent re-authentication flow, sliding session lifetimes.
- [ ] **Password Reset** — Secure token delivery via email, verification, and hash updates.

## EPIC 3 — ADMINISTRATION
- [ ] **Manage Users** — Back-office CRUD capabilities, locking/unlocking accounts.
- [ ] **Manage Roles** — RBAC setup (Admin, Support, Client), modifying access control lists (ACL).
- [ ] **View Audit Information** — System logs, tracking admin mutations, and security event streams.

## EPIC 4 — PRODUCT CATALOG
- [ ] **Browse Products** — Public/authenticated grid display, pagination, and sorting engines.
- [ ] **Search Products** — Full-text index queries, keyword filtering, and search indexing.
- [ ] **Manage Categories** — Hierarchical product taxonomy, category trees, and relations.

## EPIC 5 — ORDERS
- [ ] **Create Order** — Checkout pipeline, stock reservation, invoice generation.
- [ ] **View Order** — Customer tracking view, invoice generation, status history logs.
- [ ] **Cancel Order** — State mutation (Pending -> Canceled), inventory release procedures.

## EPIC 6 — FEEDBACK / SUPPORT
- [ ] **Support Tickets** — Issue reporting pipelines, priority sorting, ticket assignment.
- [ ] **Ticket Replies** — Internal communication thread, attachment uploads, notification triggers.
- [ ] **Ticket Resolution** — Resolving disputes, closing loops, customer feedback rating.
