import enum
import uuid
from datetime import datetime

from sqlalchemy import DateTime, Enum, ForeignKey, Integer, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class GiftStatus(str, enum.Enum):
    CREATED = "CREATED"
    FUNDING_PENDING = "FUNDING_PENDING"
    ACTIVE = "ACTIVE"
    CLAIM_PENDING = "CLAIM_PENDING"
    PAYOUT_PROCESSING = "PAYOUT_PROCESSING"
    CLAIMED = "CLAIMED"
    PAYOUT_FAILED = "PAYOUT_FAILED"
    EXPIRED = "EXPIRED"


class ClaimStatus(str, enum.Enum):
    PENDING = "PENDING"
    PROCESSING = "PROCESSING"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"


class TransactionType(str, enum.Enum):
    FUNDING = "FUNDING"
    PAYOUT = "PAYOUT"


class TransactionStatus(str, enum.Enum):
    INITIATED = "INITIATED"
    PROCESSING = "PROCESSING"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"


class Gift(Base):
    __tablename__ = "gifts"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    gift_token: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    amount: Mapped[int] = mapped_column(Integer)
    currency: Mapped[str] = mapped_column(String(3), default="INR")
    status: Mapped[GiftStatus] = mapped_column(Enum(GiftStatus), default=GiftStatus.CREATED)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    claimed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    claims: Mapped[list["GiftClaim"]] = relationship(back_populates="gift", cascade="all, delete-orphan")
    transactions: Mapped[list["Transaction"]] = relationship(back_populates="gift", cascade="all, delete-orphan")


class GiftClaim(Base):
    __tablename__ = "gift_claims"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    gift_id: Mapped[str] = mapped_column(String(36), ForeignKey("gifts.id"), index=True)
    upi_id: Mapped[str] = mapped_column(String(120))
    status: Mapped[ClaimStatus] = mapped_column(Enum(ClaimStatus), default=ClaimStatus.PENDING)
    payout_reference: Mapped[str | None] = mapped_column(String(128), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    gift: Mapped[Gift] = relationship(back_populates="claims")


class Transaction(Base):
    __tablename__ = "transactions"
    __table_args__ = (UniqueConstraint("idempotency_key", name="uq_transactions_idempotency_key"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    gift_id: Mapped[str] = mapped_column(String(36), ForeignKey("gifts.id"), index=True)
    type: Mapped[TransactionType] = mapped_column(Enum(TransactionType))
    amount: Mapped[int] = mapped_column(Integer)
    provider_reference: Mapped[str] = mapped_column(String(128), index=True)
    idempotency_key: Mapped[str] = mapped_column(String(128))
    status: Mapped[TransactionStatus] = mapped_column(Enum(TransactionStatus), default=TransactionStatus.INITIATED)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    gift: Mapped[Gift] = relationship(back_populates="transactions")
