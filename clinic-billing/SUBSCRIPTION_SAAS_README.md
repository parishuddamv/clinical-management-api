# Subscription SaaS Foundation (Clinic Billing Module)

This implementation introduces a commercial SaaS subscription layer without rewriting existing clinic workflows.

## What Was Added

- Commercial plans: `STARTER`, `PROFESSIONAL` (recommended), `BUSINESS`, `ENTERPRISE`
- Billing cycles: monthly and annual
- 30-day configurable trial (default) with Professional-level access
- Subscription statuses: `TRIAL`, `ACTIVE`, `PAST_DUE`, `GRACE_PERIOD`, `SUSPENDED`, `CANCELLED`, `EXPIRED`
- Plan features and centralized limits (`DOCTORS`, `STAFF`, `LOCATIONS`)
- Optional add-ons: WhatsApp, Pharmacy, Lab, Multi-location, Advanced Analytics, Telemedicine, API Integrations
- Subscription audit log for commercial/admin actions
- Platform/admin APIs for subscription operations and catalog controls
- Payment abstraction interface (`SubscriptionPaymentGateway`) for future Razorpay/UPI integration

## Key API Endpoints

Tenant-scoped:

- `GET /api/v1/subscriptions/current`
- `GET /api/v1/subscriptions/plans`
- `GET /api/v1/subscriptions/usage`
- `GET /api/v1/subscriptions/features/{featureCode}/availability`
- `POST /api/v1/subscriptions/upgrade`
- `POST /api/v1/subscriptions/downgrade`
- `POST /api/v1/subscriptions/cancel`
- `POST /api/v1/subscriptions/renew`
- `GET /api/v1/subscriptions/addons`
- `POST /api/v1/subscriptions/addons/{addonCode}/activate`
- `POST /api/v1/subscriptions/addons/{addonCode}/deactivate`

Platform/admin:

- `GET /api/v1/platform/subscriptions/clinics/{clinicId}`
- `POST /api/v1/platform/subscriptions/clinics/{clinicId}/plan`
- `POST /api/v1/platform/subscriptions/clinics/{clinicId}/trial/extend?days=7`
- `POST /api/v1/platform/subscriptions/clinics/{clinicId}/suspend`
- `POST /api/v1/platform/subscriptions/clinics/{clinicId}/reactivate`
- `PATCH /api/v1/platform/subscriptions/plans/{planCode}/pricing`
- `PATCH /api/v1/platform/subscriptions/plans/{planCode}/limits/{limitKey}`
- `PATCH /api/v1/platform/subscriptions/plans/{planCode}/features/{featureCode}?enabled=true|false`
- `PATCH /api/v1/platform/subscriptions/addons/{addonCode}`

## Multi-tenant Safety

- Uses existing `clinic_id` isolation through current auth context.
- Tenant APIs derive clinic from authenticated context; they do not trust frontend clinic input.
- Platform APIs require super-admin actor checks.

## Database Migration

- `clinic-billing/src/main/resources/db/migration/V3__create_subscription_commerce_tables.sql`

This migration creates and seeds plans, features, limits, add-ons, audit logs, and default Professional subscriptions for existing clinics.

## Configuration

In `clinic-billing/src/main/resources/application.yml`:

- `app.subscription.trial-days` (default: 30)
- `app.subscription.grace-days` (default: 7)

## Current Enforcement Coverage

- Billing operations now enforce subscription + feature + role permission checks.
- Staff creation enforces centralized plan limits for doctors and active staff.

## Next Integration Steps

1. Add feature checks into other modules (EMR, lab, pharmacy, reports).
2. Add gateway-level coarse route guard if centralized blocking is desired.
3. Integrate real payment provider behind `SubscriptionPaymentGateway`.
4. Extend integration tests to cover full end-to-end feature matrix.

