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
            id="trial",
            name="5-Day Trial",
            description="Experience the full VocalBharat learning platform for 5 days.",
            amount=settings.PAYMENT_TRIAL_AMOUNT,
            currency=settings.PAYMENT_CURRENCY,
            duration_days=settings.PAYMENT_TRIAL_DAYS,
            badge="Best for beginners",
        ),
        PlanInfo(
            id="monthly",
            name="Monthly Plan",
            description="Unlimited access to all lessons, AI coaching, and grammar tools.",
            amount=settings.PAYMENT_MONTHLY_AMOUNT,
            currency=settings.PAYMENT_CURRENCY,
            duration_days=30,
            badge="Most popular",
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
    plan_id = payload.plan

    # Trial plan: each user can only use it once
    if plan_id == "trial":
        used_trial = (
            db.query(Subscription)
            .filter(
                Subscription.user_id == current_user.id,
                Subscription.plan == "trial",
                Subscription.status.in_(["active", "expired"]),
            )
            .first()
        )
        if used_trial:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="You have already used your 5-day trial. Please upgrade to the Monthly plan.",
            )

    # Determine amount based on plan
    amount = (
        settings.PAYMENT_TRIAL_AMOUNT
        if plan_id == "trial"
        else settings.PAYMENT_MONTHLY_AMOUNT
    )

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

    # Activate the subscription
    now = _now()
    duration_days = (
        settings.PAYMENT_TRIAL_DAYS
        if subscription.plan == "trial"
        else 30
    )
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
                    days = settings.PAYMENT_TRIAL_DAYS if sub.plan == "trial" else 30
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
    summary="Get subscription status",
    description="Returns the current subscription plan, status, and expiry for the authenticated user.",
)
async def subscription_status(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return _build_status(current_user, db)


def _build_status(user: User, db: Session) -> SubscriptionStatusOut:
    """Build a SubscriptionStatusOut for a given user."""
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

    # Check if user has ever used trial (even if expired)
    used_trial = (
        db.query(Subscription)
        .filter(
            Subscription.user_id == user.id,
            Subscription.plan == "trial",
            Subscription.status.in_(["active", "expired"]),
        )
        .first()
        is not None
    )

    if not sub:
        return SubscriptionStatusOut(
            has_subscription=False,
            is_active=False,
            days_remaining=0,
            can_use_trial=not used_trial,
        )

    # Auto-expire subscriptions that have passed their expiry date
    if sub.status == "active" and not sub.is_active:
        sub.status = "expired"
        db.commit()

    return SubscriptionStatusOut(
        has_subscription=True,
        plan=sub.plan,
        status=sub.status,
        is_active=sub.is_active,
        starts_at=sub.starts_at,
        expires_at=sub.expires_at,
        days_remaining=sub.days_remaining,
        can_use_trial=not used_trial,
    )
