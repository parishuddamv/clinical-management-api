# Gift Platform MVP

Pre-funded, one-time cash-gift platform prototype with audit-friendly gift/claim/transaction records.

## Features

- Admin flow: create gift, fund gift, generate QR, track status.
- Recipient flow: scan QR, enter UPI ID, submit claim.
- Webhook-driven state transitions (funding and payout).
- Atomic claim transition to avoid double-claim races.
- Idempotency key support for payouts.
- HMAC verification for webhook payloads.
- In-memory rate limiting for claim endpoint.

## Tech Stack

- FastAPI (API + static screens)
- SQLAlchemy ORM
- SQLite by default (PostgreSQL-ready via `DATABASE_URL`)
- `qrcode` for QR generation

## Project Structure

- `app/main.py` - API and web routes
- `app/models.py` - DB schema
- `app/security.py` - token generation, UPI validation, webhook verification
- `app/providers.py` - payment/payout provider mocks
- `app/static/` - admin, claim, and success screens
- `tests/test_flow.py` - end-to-end API flow test

## Setup

```powershell
cd "D:\jusun\clinical-management-system\gift-platform-mvp"
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## Run

```powershell
cd "D:\jusun\clinical-management-system\gift-platform-mvp"
.\.venv\Scripts\Activate.ps1
python run.py
```

Open:

- `http://localhost:8000/` for admin dashboard
- QR points to `http://localhost:8000/claim/{giftToken}`

## Test

```powershell
cd "D:\jusun\clinical-management-system\gift-platform-mvp"
.\.venv\Scripts\Activate.ps1
pytest -q
```

## Core API

- `POST /api/gifts`
- `POST /api/gifts/{giftId}/fund`
- `GET /api/gifts/{giftToken}`
- `POST /api/gifts/{giftToken}/claim`
- `GET /api/gifts/{giftId}/status`
- `POST /api/webhooks/payment`
- `POST /api/webhooks/payout`
- `GET /api/gifts/{giftId}/qr`

## Webhook Signing

- Header: `X-Webhook-Signature`
- Signature: HMAC-SHA256 hex digest of raw JSON body using `WEBHOOK_SECRET`
- Default secret for local testing: `dev_webhook_secret`

## Environment Variables

- `DATABASE_URL` (default: `sqlite:///./gift_mvp.db`)
- `WEBHOOK_SECRET` (default: `dev_webhook_secret`)
- `PUBLIC_BASE_URL` (default: `http://localhost:8000`)
- `CLAIM_RATE_LIMIT_PER_MINUTE` (default: `30`)

