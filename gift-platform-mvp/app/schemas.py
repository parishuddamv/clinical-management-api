from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class CreateGiftRequest(BaseModel):
    amount: int = Field(ge=1, description="Amount in INR")
    currency: Literal["INR"] = "INR"
    expires_in_hours: int = Field(default=72, ge=1, le=24 * 30)


class GiftResponse(BaseModel):
    id: str
    gift_token: str
    amount: int
    currency: str
    status: str
    expires_at: datetime
    created_at: datetime


class FundGiftResponse(BaseModel):
    gift_id: str
    status: str
    payment_reference: str
    instructions: str


class GiftPublicResponse(BaseModel):
    gift_token: str
    amount: int
    currency: str
    status: str
    expires_at: datetime
    claimable: bool


class ClaimGiftRequest(BaseModel):
    upi_id: str = Field(min_length=5, max_length=120)
    idempotency_key: str | None = Field(default=None, max_length=128)


class ClaimGiftResponse(BaseModel):
    claim_id: str
    gift_status: str
    payout_reference: str
    message: str


class StatusResponse(BaseModel):
    id: str
    gift_token: str
    amount: int
    currency: str
    status: str
    expires_at: datetime
    claimed_at: datetime | None
    claims: list[dict]
    transactions: list[dict]


class WebhookEvent(BaseModel):
    event_type: str
    provider_reference: str
    status: str

