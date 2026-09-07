import hashlib
import hmac
import re
import secrets

from app.config import settings

UPI_REGEX = re.compile(r"^[a-zA-Z0-9._-]{2,}@[a-zA-Z]{2,}$")


def generate_gift_token() -> str:
    return f"GFT-{secrets.token_urlsafe(18)}"


def is_valid_upi(upi_id: str) -> bool:
    return bool(UPI_REGEX.match(upi_id))


def verify_webhook_signature(raw_body: bytes, signature: str | None) -> bool:
    if not signature:
        return False
    digest = hmac.new(settings.webhook_secret.encode("utf-8"), raw_body, hashlib.sha256).hexdigest()
    return hmac.compare_digest(digest, signature)

