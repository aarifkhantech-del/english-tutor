"""
SQLAlchemy ORM models for the VocalBharat subscription system.

Tables
------
users           — registered users (identified by email)
otp_requests    — OTP codes (hashed) with rate-limit metadata
subscriptions   — plan + lifecycle (trial / monthly)
payments        — payment attempts linked to subscriptions
"""
import hashlib
import uuid
from datetime import datetime, timezone

from sqlalchemy import (
    Boolean,
    Column,
    DateTime,
    Enum,
    ForeignKey,
    Integer,
    String,
    Text,
)
from sqlalchemy.orm import relationship

from app.core.database import Base


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _uuid() -> str:
    return str(uuid.uuid4())


# ── Users ─────────────────────────────────────────────────────────────────────

class User(Base):
    """A registered user, identified solely by email address."""
    __tablename__ = "users"

    id = Column(String(36), primary_key=True, default=_uuid)
    email = Column(String(255), unique=True, nullable=False, index=True)
    is_active = Column(Boolean, default=True, nullable=False)
    request_count = Column(Integer, default=0, nullable=False)
    created_at = Column(DateTime(timezone=True), default=_now, nullable=False)
    last_login_at = Column(DateTime(timezone=True), nullable=True)

    # Relationships
    otp_requests = relationship("OTPRequest", back_populates="user", cascade="all, delete-orphan")
    subscriptions = relationship("Subscription", back_populates="user", cascade="all, delete-orphan")
    payments = relationship("Payment", back_populates="user", cascade="all, delete-orphan")

    def __repr__(self) -> str:
        return f"<User email={self.email} requests={self.request_count} active={self.is_active}>"


# ── OTP Requests ──────────────────────────────────────────────────────────────

class OTPRequest(Base):
    """Tracks a single OTP delivery attempt for a user."""
    __tablename__ = "otp_requests"

    id = Column(String(36), primary_key=True, default=_uuid)
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    # SHA-256 hash of the plain-text OTP — never store raw codes
    code_hash = Column(String(64), nullable=False)
    created_at = Column(DateTime(timezone=True), default=_now, nullable=False)
    expires_at = Column(DateTime(timezone=True), nullable=False)
    # Number of incorrect verification attempts made against this OTP
    attempts = Column(Integer, default=0, nullable=False)
    is_used = Column(Boolean, default=False, nullable=False)

    user = relationship("User", back_populates="otp_requests")

    @staticmethod
    def hash_code(plain: str) -> str:
        return hashlib.sha256(plain.encode()).hexdigest()

    def verify(self, plain: str) -> bool:
        return self.code_hash == self.hash_code(plain)

    def __repr__(self) -> str:
        return f"<OTPRequest user={self.user_id} used={self.is_used}>"


# ── Subscriptions ─────────────────────────────────────────────────────────────

class Subscription(Base):
    """Tracks a user's subscription plan and its lifecycle."""
    __tablename__ = "subscriptions"

    id = Column(String(36), primary_key=True, default=_uuid)
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    # e.g. "monthly", "annual", etc.
    plan = Column(String(50), nullable=False, default="monthly")
    # "active" / "expired" / "cancelled" / "pending"
    status = Column(
        Enum("pending", "active", "expired", "cancelled", name="sub_status_enum"),
        default="pending",
        nullable=False,
    )
    starts_at = Column(DateTime(timezone=True), nullable=True)
    expires_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), default=_now, nullable=False)
    updated_at = Column(DateTime(timezone=True), default=_now, onupdate=_now, nullable=False)

    user = relationship("User", back_populates="subscriptions")
    payments = relationship("Payment", back_populates="subscription", cascade="all, delete-orphan")

    @property
    def is_active(self) -> bool:
        if self.status != "active":
            return False
        if self.expires_at is None:
            return False
        now = datetime.now(timezone.utc)
        exp = self.expires_at
        # Handle naive datetimes stored by SQLite
        if exp.tzinfo is None:
            from datetime import timezone as tz
            exp = exp.replace(tzinfo=tz.utc)
        return exp > now

    @property
    def days_remaining(self) -> int:
        if not self.is_active:
            return 0
        now = datetime.now(timezone.utc)
        exp = self.expires_at
        if exp.tzinfo is None:
            exp = exp.replace(tzinfo=timezone.utc)
        diff_seconds = (exp - now).total_seconds()
        import math
        return max(0, math.ceil(diff_seconds / 86400))

    def __repr__(self) -> str:
        return f"<Subscription user={self.user_id} plan={self.plan} status={self.status}>"


# ── Payments ──────────────────────────────────────────────────────────────────

class Payment(Base):
    """Records each payment attempt, whether pending, successful, or failed."""
    __tablename__ = "payments"

    id = Column(String(36), primary_key=True, default=_uuid)
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    subscription_id = Column(String(36), ForeignKey("subscriptions.id", ondelete="CASCADE"), nullable=True)
    # Which gateway handled this payment: "mock", "razorpay", "cashfree", "stripe"
    gateway = Column(String(50), nullable=False, default="mock")
    amount = Column(Integer, nullable=False)  # in smallest currency unit (paise for INR)
    currency = Column(String(3), nullable=False, default="INR")
    # IDs returned by the payment gateway
    gateway_order_id = Column(String(255), nullable=True)
    gateway_payment_id = Column(String(255), nullable=True)
    gateway_signature = Column(Text, nullable=True)
    # "pending" / "captured" / "failed" / "refunded"
    status = Column(String(50), nullable=False, default="pending")
    created_at = Column(DateTime(timezone=True), default=_now, nullable=False)
    updated_at = Column(DateTime(timezone=True), default=_now, onupdate=_now, nullable=False)

    user = relationship("User", back_populates="payments")
    subscription = relationship("Subscription", back_populates="payments")

    def __repr__(self) -> str:
        return f"<Payment amount={self.amount} gateway={self.gateway} status={self.status}>"
