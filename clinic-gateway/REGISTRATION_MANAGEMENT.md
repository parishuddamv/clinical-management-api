# Super Admin registration management

This extends the existing `clinic_users`, JWT login, `UserRegistrationService`, MVC controller and reactive auth router. It does not create another user/clinic system.

## Authentication and authorization

Send `Authorization: Bearer <application JWT>` on management requests. The signature and expiry are validated, then the **current database user** must be `APPROVED`, active, and have the server-maintained `is_super_admin=true` flag. A normal `ADMIN` is not a Super Admin. A `SUPER_ADMIN` role label alone is also insufficient: role text was historically accepted from public registration. No role/header/query parameter supplied by the browser can grant privileges. Authorization is enforced at both the security path and service layers.

Public registration cannot request `SUPER_ADMIN` or set `isSuperAdmin`. Provision the first Super Admin through an existing trusted administrative/database process, not public registration.

Google ID tokens now undergo server-side signature, audience, issuer, expiry and verified-email checks before application JWT issuance. Configure `app.google.clientId` (`GOOGLE_CLIENT_ID`). Application JWT format/signing remains unchanged. Pending/rejected/suspended users may sign in to view their own status but receive no clinic context. Clinic business routing checks current approved/active membership, so suspension also blocks previously issued JWTs. After approval, sign in again to obtain the assigned clinic context.

## Endpoints

All paths below are relative to `/api/v1/auth` and return the existing `ApiResponse` wrapper.

| Method | Path | Purpose |
|---|---|---|
| POST | `/register` | Existing public validated registration; returns 201 |
| GET | `/admin/registrations?status=NEW&search=clinic&page=0&size=20` | All registrations, optional status and case-insensitive name/email/clinic name or ID search |
| GET | `/user/{email}` | Existing complete details endpoint, accessible only to that identity or Super Admin |
| GET | `/user-status/{email}` | Existing self-or-Super-Admin status endpoint |
| GET | `/check-approval/{email}` | Existing self-or-Super-Admin approval check |
| PUT | `/admin/approve/{email}?clinicId=CLINIC_A` | Existing approval endpoint; `clinicId` optional |
| PUT | `/admin/reject/{email}?rejectionReason=...` | Existing rejection endpoint; nonblank reason required (max 2000) |
| PUT | `/admin/suspend/{email}` | Existing suspension endpoint |
| PUT | `/admin/reactivate/{email}` | Reactivate a suspended approved account |
| PUT | `/admin/review/{email}` | Move NEW or REJECTED back to PENDING for review |
| GET | `/admin/registrations/{email}/history?page=0&size=20` | Paginated audit history, newest first |

Pagination: page >= 0, size 1–100. Invalid input returns 400; unauthorized operations return 403; missing registration returns 404; duplicate/conflicting states return 409. Errors do not expose stack traces or database exceptions. The former `approvedBy` query parameter is ignored; the verified caller determines `approvedBy` and audit actor.

## Transition and duplicate policy

- Public registration creates NEW/inactive with preserved clinic role and a REGISTERED audit record (`PUBLIC_REGISTRATION` is not a verified actor). `registrationDetails` holds the complete validated submission as JSON text, including optional demo/practice fields.
- Only NEW/PENDING can be approved/rejected. Approval sets active, approvedAt/approvedBy and clears the current rejection reason. If no clinicId is supplied, reuse the association or generate a unique `CLINIC_<UUID>` identifier, matching the existing identifier-based architecture.
- Only APPROVED can be suspended; only SUSPENDED can be reactivated.
- Rejection retains the user and reason and makes the account inactive. Repeated public registration never overwrites the record. A Super Admin can reopen it via `/admin/review/{email}`; historical rejection reasons remain in the audit table.
- Email comparison is normalized (trim/lowercase), and a database unique index handles concurrent duplicates. Each state change takes a row lock and writes audit information in the same transaction. Failed audits roll back the state change.

## Migration and rollout

`V2__registration_management_audit.sql` in the gateway migration directory adds one nullable `registration_details` column, a normalized unique email index, and `registration_status_history` with a user foreign key. V1 is untouched. Existing records receive a clearly labelled MIGRATED snapshot, not fabricated historical events. Previously unpersisted optional form fields cannot be recovered.

1. Back up the database; check and manually reconcile case/whitespace-equivalent duplicate emails. V2 intentionally fails without deleting/merging records if any exist.
2. Apply gateway Flyway migrations before rolling out other services that scan the shared user entity. Keep `flyway_schema_history_gateway` as the gateway history table.
3. **Rotate the shared JWT secret across all services and force re-login at rollout.** The prior Google decoder did not verify signatures; do not continue trusting tokens issued before this fix. Use a strong private production secret, never the repository fallback.
4. Supply the Google client ID and allow outbound HTTPS to Google's signing-key endpoint. Only verified Google ID tokens are now accepted.
5. Keep internal microservices private behind the gateway. The current-state business-route check is at the gateway; this change does not implement a platform-wide RBAC rewrite of every clinical endpoint.
6. The frontend must attach JWTs to the existing detail/status endpoints (previously public), remove editable `approvedBy`, and handle 400/403/409 appropriately.

## Tests

From the repository root:

```powershell
mvn -B -pl clinic-gateway -am test
mvn -B clean verify
```

Tests use isolated H2 for HTTP/JPA integration and disposable embedded PostgreSQL for actual Flyway scripts; no production database or credentials are used. PostgreSQL test binaries are downloaded by Maven and require permission to launch a local process. Google verification tests use locally generated RSA keys rather than real accounts.
