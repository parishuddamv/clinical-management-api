from datetime import datetime, timedelta
from io import BytesIO
import uuid

import qrcode
from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import FileResponse, StreamingResponse
from fastapi.staticfiles import StaticFiles
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.config import settings
from app.database import Base, engine, get_db_session
from app.models import (
    ClaimStatus,
    Gift,
    GiftClaim,
    GiftStatus,
    Transaction,
    TransactionStatus,
    TransactionType,
)
from app.providers import PaymentProviderMock, PayoutProviderMock
from app.rate_limit import InMemoryRateLimiter, get_client_ip
from app.schemas import (
    ClaimGiftRequest,
    ClaimGiftResponse,
    CreateGiftRequest,
    FundGiftResponse,
    GiftPublicResponse,
    GiftResponse,
    StatusResponse,
    WebhookEvent,
)
from app.security import generate_gift_token, is_valid_upi, verify_webhook_signature

app = FastAPI(title=settings.app_name)
app.mount("/static", StaticFiles(directory="app/static"), name="static")
Base.metadata.create_all(bind=engine)

rate_limiter = InMemoryRateLimiter(settings.claim_rate_limit_per_minute)


def _utcnow() -> datetime:
    return datetime.utcnow()


def _expire_if_needed(db: Session, gift: Gift) -> Gift:
    if gift.status in {GiftStatus.CLAIMED, GiftStatus.EXPIRED}:
        return gift
    if gift.expires_at <= _utcnow():
        gift.status = GiftStatus.EXPIRED
        db.add(gift)
        db.commit()
        db.refresh(gift)
    return gift


@app.get("/", include_in_schema=False)
def root() -> FileResponse:
    return FileResponse("app/static/admin.html")


@app.get("/claim/{gift_token}", include_in_schema=False)
def claim_page(gift_token: str) -> FileResponse:
    return FileResponse("app/static/gift.html")


@app.get("/success", include_in_schema=False)
def success_page() -> FileResponse:
    return FileResponse("app/static/success.html")


@app.post("/api/gifts", response_model=GiftResponse)
def create_gift(payload: CreateGiftRequest, db: Session = Depends(get_db_session)):
    gift = Gift(
        gift_token=generate_gift_token(),
        amount=payload.amount,
        currency=payload.currency,
        status=GiftStatus.CREATED,
        expires_at=_utcnow() + timedelta(hours=payload.expires_in_hours),
    )
    db.add(gift)
    db.commit()
    db.refresh(gift)
    return gift


@app.post("/api/gifts/{gift_id}/fund", response_model=FundGiftResponse)
def fund_gift(gift_id: str, db: Session = Depends(get_db_session)):
    gift = db.get(Gift, gift_id)
    if not gift:
        raise HTTPException(status_code=404, detail="Gift not found")

    gift = _expire_if_needed(db, gift)
    if gift.status not in {GiftStatus.CREATED, GiftStatus.FUNDING_PENDING}:
        raise HTTPException(status_code=409, detail=f"Gift cannot be funded from status {gift.status.value}")

    payment_reference = PaymentProviderMock.create_funding_intent(gift.id, gift.amount, gift.currency)
    gift.status = GiftStatus.FUNDING_PENDING
    tx = Transaction(
        gift_id=gift.id,
        type=TransactionType.FUNDING,
        amount=gift.amount,
        provider_reference=payment_reference,
        idempotency_key=f"fund-{gift.id}-{uuid.uuid4().hex[:8]}",
        status=TransactionStatus.INITIATED,
    )
    db.add_all([gift, tx])
    db.commit()

    return FundGiftResponse(
        gift_id=gift.id,
        status=gift.status.value,
        payment_reference=payment_reference,
        instructions="Simulate provider callback by POSTing /api/webhooks/payment with payment.succeeded",
    )


@app.get("/api/gifts/{gift_token}", response_model=GiftPublicResponse)
def get_gift_public(gift_token: str, db: Session = Depends(get_db_session)):
    gift = db.scalar(select(Gift).where(Gift.gift_token == gift_token))
    if not gift:
        raise HTTPException(status_code=404, detail="Gift not found")

    gift = _expire_if_needed(db, gift)
    claimable = gift.status == GiftStatus.ACTIVE and gift.expires_at > _utcnow()

    return GiftPublicResponse(
        gift_token=gift.gift_token,
        amount=gift.amount,
        currency=gift.currency,
        status=gift.status.value,
        expires_at=gift.expires_at,
        claimable=claimable,
    )


@app.post("/api/gifts/{gift_token}/claim", response_model=ClaimGiftResponse)
def claim_gift(gift_token: str, payload: ClaimGiftRequest, request: Request, db: Session = Depends(get_db_session)):
    rate_limiter.check(f"claim:{get_client_ip(request)}")

    if not is_valid_upi(payload.upi_id):
        raise HTTPException(status_code=400, detail="Invalid UPI ID format")

    gift = db.scalar(select(Gift).where(Gift.gift_token == gift_token))
    if not gift:
        raise HTTPException(status_code=404, detail="Gift not found")

    gift = _expire_if_needed(db, gift)
    if gift.status != GiftStatus.ACTIVE:
        raise HTTPException(status_code=409, detail=f"Gift cannot be claimed in status {gift.status.value}")

    claim_id = str(uuid.uuid4())
    idem_key = payload.idempotency_key or f"claim-{gift.id}-{claim_id[:8]}"

    # Atomic status transition prevents two recipients from claiming the same gift.
    moved = db.execute(
        update(Gift)
        .where(Gift.id == gift.id, Gift.status == GiftStatus.ACTIVE)
        .values(status=GiftStatus.CLAIM_PENDING)
        .returning(Gift.id)
    ).first()

    if not moved:
        db.rollback()
        raise HTTPException(status_code=409, detail="Gift was already claimed or is being processed")

    payout_reference = PayoutProviderMock.create_payout(gift.id, payload.upi_id, gift.amount, idem_key)

    claim = GiftClaim(
        id=claim_id,
        gift_id=gift.id,
        upi_id=payload.upi_id,
        status=ClaimStatus.PROCESSING,
        payout_reference=payout_reference,
    )
    payout_tx = Transaction(
        gift_id=gift.id,
        type=TransactionType.PAYOUT,
        amount=gift.amount,
        provider_reference=payout_reference,
        idempotency_key=idem_key,
        status=TransactionStatus.PROCESSING,
    )

    db.execute(update(Gift).where(Gift.id == gift.id).values(status=GiftStatus.PAYOUT_PROCESSING))
    db.add_all([claim, payout_tx])
    db.commit()

    return ClaimGiftResponse(
        claim_id=claim.id,
        gift_status=GiftStatus.PAYOUT_PROCESSING.value,
        payout_reference=payout_reference,
        message="Claim accepted. Awaiting payout webhook confirmation.",
    )


@app.get("/api/gifts/{gift_id}/status", response_model=StatusResponse)
def get_gift_status(gift_id: str, db: Session = Depends(get_db_session)):
    gift = db.get(Gift, gift_id)
    if not gift:
        raise HTTPException(status_code=404, detail="Gift not found")

    gift = _expire_if_needed(db, gift)
    claims = db.scalars(select(GiftClaim).where(GiftClaim.gift_id == gift.id)).all()
    transactions = db.scalars(select(Transaction).where(Transaction.gift_id == gift.id)).all()

    return StatusResponse(
        id=gift.id,
        gift_token=gift.gift_token,
        amount=gift.amount,
        currency=gift.currency,
        status=gift.status.value,
        expires_at=gift.expires_at,
        claimed_at=gift.claimed_at,
        claims=[
            {
                "id": c.id,
                "upi_id": c.upi_id,
                "status": c.status.value,
                "payout_reference": c.payout_reference,
                "created_at": c.created_at,
            }
            for c in claims
        ],
        transactions=[
            {
                "id": t.id,
                "type": t.type.value,
                "amount": t.amount,
                "provider_reference": t.provider_reference,
                "idempotency_key": t.idempotency_key,
                "status": t.status.value,
                "created_at": t.created_at,
            }
            for t in transactions
        ],
    )


@app.post("/api/webhooks/payment")
async def payment_webhook(
    event: WebhookEvent,
    request: Request,
    x_webhook_signature: str | None = Header(default=None),
    db: Session = Depends(get_db_session),
):
    raw = await request.body()
    if not verify_webhook_signature(raw, x_webhook_signature):
        raise HTTPException(status_code=401, detail="Invalid webhook signature")

    tx = db.scalar(
        select(Transaction).where(
            Transaction.provider_reference == event.provider_reference,
            Transaction.type == TransactionType.FUNDING,
        )
    )
    if not tx:
        raise HTTPException(status_code=404, detail="Funding transaction not found")

    gift = db.get(Gift, tx.gift_id)
    if not gift:
        raise HTTPException(status_code=404, detail="Gift not found")

    if event.event_type == "payment.succeeded":
        tx.status = TransactionStatus.SUCCESS
        if gift.status in {GiftStatus.CREATED, GiftStatus.FUNDING_PENDING}:
            gift.status = GiftStatus.ACTIVE
    elif event.event_type == "payment.failed":
        tx.status = TransactionStatus.FAILED
        if gift.status == GiftStatus.FUNDING_PENDING:
            gift.status = GiftStatus.CREATED
    else:
        raise HTTPException(status_code=400, detail="Unsupported payment event")

    db.add_all([tx, gift])
    db.commit()
    return {"ok": True, "gift_status": gift.status.value}


@app.post("/api/webhooks/payout")
async def payout_webhook(
    event: WebhookEvent,
    request: Request,
    x_webhook_signature: str | None = Header(default=None),
    db: Session = Depends(get_db_session),
):
    raw = await request.body()
    if not verify_webhook_signature(raw, x_webhook_signature):
        raise HTTPException(status_code=401, detail="Invalid webhook signature")

    tx = db.scalar(
        select(Transaction).where(
            Transaction.provider_reference == event.provider_reference,
            Transaction.type == TransactionType.PAYOUT,
        )
    )
    if not tx:
        raise HTTPException(status_code=404, detail="Payout transaction not found")

    gift = db.get(Gift, tx.gift_id)
    claim = db.scalar(select(GiftClaim).where(GiftClaim.gift_id == tx.gift_id, GiftClaim.payout_reference == tx.provider_reference))
    if not gift or not claim:
        raise HTTPException(status_code=404, detail="Gift claim not found")

    if event.event_type == "payout.succeeded":
        tx.status = TransactionStatus.SUCCESS
        claim.status = ClaimStatus.SUCCESS
        if gift.status != GiftStatus.CLAIMED:
            gift.status = GiftStatus.CLAIMED
            gift.claimed_at = _utcnow()
    elif event.event_type == "payout.failed":
        tx.status = TransactionStatus.FAILED
        claim.status = ClaimStatus.FAILED
        if gift.status != GiftStatus.CLAIMED:
            gift.status = GiftStatus.PAYOUT_FAILED
    else:
        raise HTTPException(status_code=400, detail="Unsupported payout event")

    db.add_all([tx, claim, gift])
    db.commit()
    return {"ok": True, "gift_status": gift.status.value}


@app.get("/api/gifts/{gift_id}/qr")
def get_gift_qr(gift_id: str, db: Session = Depends(get_db_session)):
    gift = db.get(Gift, gift_id)
    if not gift:
        raise HTTPException(status_code=404, detail="Gift not found")

    claim_url = f"{settings.public_base_url}/claim/{gift.gift_token}"
    img = qrcode.make(claim_url)
    buffer = BytesIO()
    img.save(buffer, format="PNG")
    buffer.seek(0)

    return StreamingResponse(buffer, media_type="image/png")

