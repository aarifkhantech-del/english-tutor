from __future__ import annotations
"""
Razorpay Payment API Routes.

Endpoints:
- POST /api/create-order: Creates a new Razorpay order
- POST /api/verify-payment: Verifies the HMAC-SHA256 signature for completed payments
- GET  /api/payment-config: Exposes public configuration (key_id, currency) to frontend
"""
import hashlib
import hmac
import logging
import uuid
from datetime import datetime, timedelta, timezone
from typing import Any, Optional

from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field

from app.core.config import settings
from app.core.mongodb import get_mongo_db
from app.services.mongo_repositories import get_payment_by_order_id

logger = logging.getLogger("english_tutor.payment_routes")
router = APIRouter(prefix="/api", tags=["Razorpay Checkout"])


class CreateOrderIn(BaseModel):
    """Request payload for creating a Razorpay order."""
    amount: int = Field(..., description="Amount in paise (minimum 100 paise = ₹1.00)")
    currency: str = Field(default="INR", description="Currency code (e.g. INR)")
    receipt: Optional[str] = Field(default=None, description="Optional receipt identifier")
    notes: Optional[dict[str, Any]] = Field(default=None, description="Optional order metadata notes")


class CreateOrderOut(BaseModel):
    """Response payload returned when Razorpay order is created."""
    order_id: str
    amount: int
    currency: str
    key_id: str


class VerifyPaymentIn(BaseModel):
    """Request payload for signature verification."""
    razorpay_order_id: Optional[str] = None
    razorpay_payment_id: Optional[str] = None
    razorpay_signature: Optional[str] = None
    order_id: Optional[str] = None
    payment_id: Optional[str] = None
    signature: Optional[str] = None


class VerifyPaymentOut(BaseModel):
    """Response payload returned upon signature verification."""
    status: str
    message: str
    order_id: str
    payment_id: str


class PaymentConfigOut(BaseModel):
    """Public Razorpay configuration for frontend checkout modal."""
    key_id: str
    currency: str
    monthly_amount_paise: int


def _get_razorpay_client():
    """Instantiate and return a configured Razorpay client."""
    if not settings.RAZORPAY_KEY_ID or not settings.RAZORPAY_KEY_SECRET:
        logger.error("Razorpay credentials are not configured in environment variables.")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Razorpay API credentials are not configured.",
        )

    try:
        import razorpay
        return razorpay.Client(auth=(settings.RAZORPAY_KEY_ID, settings.RAZORPAY_KEY_SECRET))
    except ImportError as err:
        logger.error("Razorpay SDK is not installed: %s", err)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Razorpay SDK is not installed on the server.",
        ) from err


@router.get(
    "/payment-config",
    response_model=PaymentConfigOut,
    summary="Get public payment configuration",
    description="Returns public Razorpay key ID and defaults for frontend SDK initialization.",
)
async def get_payment_config():
    return PaymentConfigOut(
        key_id=settings.RAZORPAY_KEY_ID,
        currency=settings.PAYMENT_CURRENCY,
        monthly_amount_paise=settings.PAYMENT_MONTHLY_AMOUNT * 100,
    )


@router.post(
    "/create-order",
    response_model=CreateOrderOut,
    summary="Create Razorpay Order",
    description="Validates amount (minimum 100 paise) and creates an order on Razorpay.",
)
async def create_order(payload: CreateOrderIn):
    if payload.amount < 100:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Amount must be at least 100 paise (₹1.00).",
        )

    client = _get_razorpay_client()
    receipt = payload.receipt or f"rcpt_{uuid.uuid4().hex[:12]}"
    notes = payload.notes or {"source": "web_checkout"}

    order_params = {
        "amount": payload.amount,
        "currency": payload.currency.upper(),
        "receipt": receipt,
        "notes": notes,
    }

    try:
        order = client.order.create(data=order_params)
        logger.info("Razorpay order created successfully: %s (%d %s)", order["id"], payload.amount, payload.currency)
        return CreateOrderOut(
            order_id=order["id"],
            amount=int(order["amount"]),
            currency=str(order["currency"]),
            key_id=settings.RAZORPAY_KEY_ID,
        )
    except Exception as exc:
        err_msg = str(exc)
        logger.error("Failed to create Razorpay order: %s", err_msg)
        if "Authentication failed" in err_msg or "Unauthorized" in err_msg or "401" in err_msg:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Razorpay authentication failed: {err_msg}",
            ) from exc
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to create Razorpay order: {err_msg}",
        ) from exc


@router.post(
    "/verify-payment",
    response_model=VerifyPaymentOut,
    summary="Verify Payment Signature",
    description="Verifies the HMAC-SHA256 signature generated by Razorpay Checkout.",
)
async def verify_payment(payload: VerifyPaymentIn):
    order_id = payload.razorpay_order_id or payload.order_id
    payment_id = payload.razorpay_payment_id or payload.payment_id
    signature = payload.razorpay_signature or payload.signature

    if not order_id or not payment_id or not signature:
        missing = []
        if not order_id:
            missing.append("order_id")
        if not payment_id:
            missing.append("payment_id")
        if not signature:
            missing.append("signature")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Missing required fields: {', '.join(missing)}",
        )

    if not settings.RAZORPAY_KEY_SECRET:
        logger.error("RAZORPAY_KEY_SECRET is not configured.")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Server payment secret is not configured.",
        )

    message = f"{order_id}|{payment_id}"
    secret_bytes = settings.RAZORPAY_KEY_SECRET.encode("utf-8")
    expected_signature = hmac.new(secret_bytes, message.encode("utf-8"), hashlib.sha256).hexdigest()

    if not hmac.compare_digest(expected_signature, signature):
        logger.warning("Razorpay signature verification failed for order %s and payment %s", order_id, payment_id)
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Payment verification failed: Signature mismatch.",
        )

    db = get_mongo_db()
    if db is not None:
        db.payments.update_one(
            {"order_id": order_id},
            {
                "$set": {
                    "status": "captured",
                    "payment_id": payment_id,
                    "gateway_signature": signature,
                    "updated_at": datetime.now(timezone.utc),
                }
            },
        )

    logger.info("Payment signature verified successfully: order=%s, payment=%s", order_id, payment_id)
    return VerifyPaymentOut(
        status="success",
        message="Payment verified successfully.",
        order_id=order_id,
        payment_id=payment_id,
    )
