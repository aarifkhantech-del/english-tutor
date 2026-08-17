from __future__ import annotations
"""
Subscription and payment API routes.

Endpoints
---------
GET  /subscription/plans      — list available plans (no auth required)
POST /subscription/initiate   — create a payment order for a chosen plan
POST /subscription/confirm    — confirm payment and activate subscription
POST /subscription/webhook    — receive payment gateway webhook events
GET  /subscription/status     — return the current user's subscription state
"""
import json
import logging
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Body, Depends, Header, HTTPException, Request, status
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.core.security import get_current_user
from app.models.db_models import Payment, Subscription, User
from app.models.schemas import (
    CheckOrderIn,
    ConfirmPaymentIn,
    InitiatePaymentIn,
    InitiatePaymentOut,
    PlanInfo,
    PlansOut,
    SubscriptionStatusOut,
)
from app.services.payment.factory import gateway

logger = logging.getLogger("english_tutor.subscription")
router = APIRouter(prefix="/subscription", tags=["Subscription"])


def _now() -> datetime:
    return datetime.now(timezone.utc)


# ── Plan catalogue ────────────────────────────────────────────────────────────

def _get_plans() -> list[PlanInfo]:
    return [
        PlanInfo(
            id="monthly",
            name="Monthly Pro Plan",
            description="Unlimited access to AI Spoken English tutor, pronunciation feedback, and grammar tools.",
            amount=settings.PAYMENT_MONTHLY_AMOUNT,
            currency=settings.PAYMENT_CURRENCY,
            duration_days=30,
            badge="Recommended",
        ),
    ]


@router.get(
    "/plans",
    response_model=PlansOut,
    summary="List subscription plans",
    description="Returns all available subscription plans with pricing. No authentication required.",
)
async def list_plans():
    return PlansOut(plans=_get_plans())


# ── Initiate payment ──────────────────────────────────────────────────────────

@router.post(
    "/initiate",
    response_model=InitiatePaymentOut,
    summary="Initiate a payment",
    description=(
        "Creates a payment order for the chosen plan. "
        "Returns gateway order details that the frontend uses to open the payment modal."
    ),
)
async def initiate_payment(
    payload: InitiatePaymentIn,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    plan_id = payload.plan or "monthly"
    amount = settings.PAYMENT_MONTHLY_AMOUNT

    gw = gateway()
    order = await gw.create_order(amount, settings.PAYMENT_CURRENCY, plan_id)

    # Create a pending subscription record
    subscription = Subscription(
        user_id=current_user.id,
        plan=plan_id,
        status="pending",
    )
    db.add(subscription)
    db.flush()  # get subscription.id without committing

    # Record the pending payment
    payment = Payment(
        user_id=current_user.id,
        subscription_id=subscription.id,
        gateway=settings.PAYMENT_GATEWAY,
        amount=amount,
        currency=settings.PAYMENT_CURRENCY,
        gateway_order_id=order.order_id,
        status="pending",
    )
    db.add(payment)
    db.commit()

    logger.info(
        "Payment initiated | user=%s | plan=%s | order=%s | amount=₹%d",
        current_user.email, plan_id, order.order_id, amount,
    )

    return InitiatePaymentOut(
        order_id=order.order_id,
        subscription_id=subscription.id,
        gateway=settings.PAYMENT_GATEWAY,
        amount=amount,
        currency=order.currency,
        gateway_key=order.gateway_key,
    )


# ── Confirm payment ───────────────────────────────────────────────────────────

@router.post(
    "/confirm",
    response_model=SubscriptionStatusOut,
    summary="Confirm a completed payment",
    description=(
        "Called by the frontend after the user successfully completes payment in the checkout modal. "
        "Verifies the payment with the gateway and activates the subscription."
    ),
)
async def confirm_payment(
    payload: ConfirmPaymentIn,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    # Find the pending payment record
    payment: Payment | None = (
        db.query(Payment)
        .filter(
            Payment.gateway_order_id == payload.order_id,
            Payment.user_id == current_user.id,
            Payment.status == "pending",
        )
        .first()
    )
    if not payment:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No pending payment found for this order. It may have already been confirmed.",
        )

    subscription: Subscription = (
        db.query(Subscription).filter(Subscription.id == payment.subscription_id).first()
    )

    # Verify the payment with the gateway
    gw = gateway()
    result = await gw.verify_payment(payload.order_id, payload.payment_id, payload.signature)

    if not result.success:
        payment.status = "failed"
        subscription.status = "cancelled"
        db.commit()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Payment verification failed: {result.message}",
        )

    # Activate the subscription (30 days)
    now = _now()
    duration_days = 30
    subscription.status = "active"
    subscription.starts_at = now
    subscription.expires_at = now + timedelta(days=duration_days)

    # Mark payment as captured
    payment.status = "captured"
    payment.gateway_payment_id = result.payment_id
    payment.gateway_signature = payload.signature

    db.commit()

    logger.info(
        "Subscription activated | user=%s | plan=%s | expires=%s",
        current_user.email, subscription.plan, subscription.expires_at.isoformat(),
    )

    return _build_status(current_user, db)


# ── Check order status directly (e.g. for UPI QR Scan) ───────────────────────

@router.post(
    "/check-order",
    response_model=SubscriptionStatusOut,
    summary="Check order status with payment gateway",
    description=(
        "Directly queries Razorpay to verify if an order was paid (e.g. via UPI QR scan). "
        "Activates the subscription immediately if payment is verified on the gateway."
    ),
)
async def check_order_status_endpoint(
    payload: CheckOrderIn = Body(default_factory=CheckOrderIn),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    query = db.query(Payment).filter(Payment.user_id == current_user.id)
    if payload.order_id:
        payment: Payment | None = query.filter(Payment.gateway_order_id == payload.order_id).first()
    else:
        # Check the latest pending payment
        payment = query.filter(Payment.status == "pending").order_by(Payment.created_at.desc()).first()

    if not payment:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No order found to check. If you made a payment, please ensure you are signed in with the same email.",
        )

    subscription: Subscription | None = (
        db.query(Subscription).filter(Subscription.id == payment.subscription_id).first()
    )

    # If already active, return current status
    if payment.status == "captured" and subscription and subscription.status == "active":
        return _build_status(current_user, db)

    # Query gateway API directly
    gw = gateway()
    result = await gw.check_order_status(payment.gateway_order_id)

    if not result.success:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=result.message,
        )

    # Activate subscription
    now = _now()
    duration_days = 30
    if subscription:
        subscription.status = "active"
        subscription.starts_at = now
        subscription.expires_at = now + timedelta(days=duration_days)

    payment.status = "captured"
    payment.gateway_payment_id = result.payment_id
    db.commit()

    logger.info(
        "Subscription activated via check-order | user=%s | order=%s | payment=%s",
        current_user.email, payment.gateway_order_id, result.payment_id
    )

    return _build_status(current_user, db)



# ── Webhook handler ───────────────────────────────────────────────────────────

@router.post(
    "/webhook",
    status_code=status.HTTP_200_OK,
    summary="Payment gateway webhook",
    description=(
        "Receives payment lifecycle events directly from the payment gateway. "
        "Validates the gateway signature before processing any event."
    ),
)
async def payment_webhook(request: Request, db: Session = Depends(get_db)):
    body = await request.body()
    headers = dict(request.headers)

    gw = gateway()
    if not gw.verify_webhook_signature(body, headers):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Webhook signature validation failed.",
        )

    try:
        data = json.loads(body)
    except json.JSONDecodeError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid JSON payload in webhook body.",
        )

    event = data.get("event", "")
    order_id = data.get("order_id", "")
    payment_id = data.get("payment_id", "")

    logger.info("Webhook received | event=%s | order=%s", event, order_id)

    if event in ("payment.captured", "PAYMENT_SUCCESS") and order_id:
        payment = db.query(Payment).filter(Payment.gateway_order_id == order_id).first()
        if payment and payment.status == "pending":
            payment.status = "captured"
            payment.gateway_payment_id = payment_id
            if payment.subscription_id:
                sub = db.query(Subscription).filter(Subscription.id == payment.subscription_id).first()
                if sub and sub.status == "pending":
                    now = _now()
                    days = 30
                    sub.status = "active"
                    sub.starts_at = now
                    sub.expires_at = now + timedelta(days=days)
                    logger.info("Subscription activated via webhook | sub=%s", sub.id)
            db.commit()

    return {"status": "ok"}


# ── Subscription status ───────────────────────────────────────────────────────

@router.get(
    "/status",
    response_model=SubscriptionStatusOut,
    summary="Get subscription status and quota",
    description="Returns the current subscription plan, active state, and remaining free requests quota.",
)
async def subscription_status(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return _build_status(current_user, db)


def _build_status(user: User, db: Session) -> SubscriptionStatusOut:
    """Build a SubscriptionStatusOut with usage quota for a given user."""
    # Get the most recent non-cancelled subscription
    sub: Subscription | None = (
        db.query(Subscription)
        .filter(
            Subscription.user_id == user.id,
            Subscription.status != "cancelled",
        )
        .order_by(Subscription.created_at.desc())
        .first()
    )

    requests_used = int(getattr(user, "request_count", 0))
    requests_limit = settings.FREE_REQUESTS_LIMIT
    requests_remaining = max(0, requests_limit - requests_used)

    if not sub:
        return SubscriptionStatusOut(
            has_subscription=False,
            is_active=False,
            days_remaining=0,
            requests_used=requests_used,
            requests_limit=requests_limit,
            requests_remaining=requests_remaining,
            quota_exceeded=(requests_used >= requests_limit),
        )

    # Auto-expire subscriptions that have passed their expiry date
    if sub.status == "active" and not sub.is_active:
        sub.status = "expired"
        db.commit()

    is_active = sub.is_active
    quota_exceeded = (requests_used >= requests_limit) and not is_active

    return SubscriptionStatusOut(
        has_subscription=True,
        plan=sub.plan,
        status=sub.status,
        is_active=is_active,
        starts_at=sub.starts_at,
        expires_at=sub.expires_at,
        days_remaining=sub.days_remaining,
        requests_used=requests_used,
        requests_limit=requests_limit,
        requests_remaining=requests_remaining,
        quota_exceeded=quota_exceeded,
    )
