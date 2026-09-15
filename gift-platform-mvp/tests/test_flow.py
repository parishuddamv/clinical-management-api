import hashlib
import hmac
import json
import os

from fastapi.testclient import TestClient

os.environ["DATABASE_URL"] = "sqlite:///./test_gift_mvp.db"
os.environ["WEBHOOK_SECRET"] = "dev_webhook_secret"

from app.main import app  # noqa: E402
from app.security import UPI_REGEX  # noqa: E402


client = TestClient(app)


def sign(payload: bytes) -> str:
    return hmac.new(b"dev_webhook_secret", payload, hashlib.sha256).hexdigest()


def test_upi_regex_accepts_normal_upi():
    assert UPI_REGEX.match("alice123@oksbi")


def test_end_to_end_gift_claim_flow():
    create = client.post("/api/gifts", json={"amount": 500, "currency": "INR", "expires_in_hours": 48})
    assert create.status_code == 200
    gift = create.json()

    fund = client.post(f"/api/gifts/{gift['id']}/fund")
    assert fund.status_code == 200
    pay_ref = fund.json()["payment_reference"]

    payment_payload = {
        "event_type": "payment.succeeded",
        "provider_reference": pay_ref,
        "status": "SUCCESS",
    }
    payment_body = json.dumps(payment_payload, separators=(",", ":")).encode("utf-8")
    payment = client.post(
        "/api/webhooks/payment",
        content=payment_body,
        headers={"Content-Type": "application/json", "X-Webhook-Signature": sign(payment_body)},
    )
    assert payment.status_code == 200
    assert payment.json()["gift_status"] == "ACTIVE"

    public = client.get(f"/api/gifts/{gift['gift_token']}")
    assert public.status_code == 200
    assert public.json()["claimable"] is True

    claim = client.post(f"/api/gifts/{gift['gift_token']}/claim", json={"upi_id": "alice@oksbi"})
    assert claim.status_code == 200
    payout_ref = claim.json()["payout_reference"]

    payout_payload = {
        "event_type": "payout.succeeded",
        "provider_reference": payout_ref,
        "status": "SUCCESS",
    }
    payout_body = json.dumps(payout_payload, separators=(",", ":")).encode("utf-8")
    payout = client.post(
        "/api/webhooks/payout",
        content=payout_body,
        headers={"Content-Type": "application/json", "X-Webhook-Signature": sign(payout_body)},
    )
    assert payout.status_code == 200
    assert payout.json()["gift_status"] == "CLAIMED"

    status = client.get(f"/api/gifts/{gift['id']}/status")
    assert status.status_code == 200
    body = status.json()
    assert body["status"] == "CLAIMED"
    assert any(item["type"] == "FUNDING" and item["status"] == "SUCCESS" for item in body["transactions"])
    assert any(item["type"] == "PAYOUT" and item["status"] == "SUCCESS" for item in body["transactions"])

