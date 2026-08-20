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
from typing import Any

from fastapi import APIRouter, Body, Depends, HTTPException, Request, status

from app.core.config import settings
from app.core.mongodb import get_mongo_db
from app.core.security import get_current_user, get_optional_user
from app.services.mongo_repositories import get_user_request_count
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


def _as_utc(value: Any) -> datetime | None:
    """Normalize MongoDB's timezone-naive BSON dates before Python compares them."""
    if not isinstance(value, datetime):
        return None
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)


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


@router.post(
    "/initiate",
    response_model=InitiatePaymentOut,
    summary="Initiate a payment",
    description="Creates a payment order for the chosen plan.",
)
async def initiate_payment(
    payload: InitiatePaymentIn,
    current_user=Depends(get_current_user),
):
    plan_id = payload.plan or "monthly"
    amount = settings.PAYMENT_MONTHLY_AMOUNT
    gw = gateway()
    order = await gw.create_order(amount, settings.PAYMENT_CURRENCY, plan_id)

    db = get_mongo_db()
    subscription = {
        "id": f"sub_{datetime.now(timezone.utc).strftime('%Y%m%d%H%M%S%f')}",
        "user_id": current_user.id,
        "email": current_user.email,
        "plan": plan_id,
        "status": "pending",
        "is_active": False,
        "start_date": None,
        "end_date": None,
        "created_at": _now(),
        "updated_at": _now(),
    }
    if db is not None:
        db.subscriptions.insert_one(subscription)
        db.payments.insert_one({
            "id": f"pay_{datetime.now(timezone.utc).strftime('%Y%m%d%H%M%S%f')}",
            "user_id": current_user.id,
            "subscription_id": subscription["id"],
            "order_id": order.order_id,
            "amount": amount,
            "currency": settings.PAYMENT_CURRENCY,
            "status": "pending",
            "gateway": settings.PAYMENT_GATEWAY,
            "created_at": _now(),
            "updated_at": _now(),
        })

    logger.info("Payment initiated | user=%s | plan=%s | order=%s | amount=₹%d", current_user.email, plan_id, order.order_id, amount)
    return InitiatePaymentOut(
        order_id=order.order_id,
        subscription_id=subscription["id"],
        gateway=settings.PAYMENT_GATEWAY,
        amount=amount,
        currency=order.currency,
        gateway_key=order.gateway_key,
    )


@router.post(
    "/confirm",
    response_model=SubscriptionStatusOut,
    summary="Confirm a completed payment",
)
async def confirm_payment(
    payload: ConfirmPaymentIn,
    current_user=Depends(get_current_user),
):
    db = get_mongo_db()
    if db is None:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="MongoDB is not configured.")

    payment = db.payments.find_one({"order_id": payload.order_id, "user_id": current_user.id, "status": "pending"})
    if not payment:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No pending payment found for this order.")

    gw = gateway()
    result = await gw.verify_payment(payload.order_id, payload.payment_id, payload.signature)
    if not result.success:
        # Do not cancel the pending record solely because the SDK callback did
        # not include a valid signature. The app/webhook can still verify a
        # captured Razorpay order directly.
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Payment verification failed: {result.message}")

    now = _now()
    duration_days = 30
    sub_filter = {"id": payment.get("subscription_id")}
    db.subscriptions.update_one(sub_filter, {"$set": {"status": "active", "is_active": True, "start_date": now, "end_date": now + timedelta(days=duration_days), "updated_at": now}})
    db.payments.update_one({"id": payment["id"]}, {"$set": {"status": "captured", "payment_id": result.payment_id, "gateway_signature": payload.signature, "updated_at": now}})

    logger.info("Subscription activated | user=%s | order=%s | expires=%s", current_user.email, payload.order_id, (now + timedelta(days=duration_days)).isoformat())
    return _build_status(current_user)


@router.post(
    "/check-order",
    response_model=SubscriptionStatusOut,
    summary="Check order status with payment gateway",
)
async def check_order_status_endpoint(
    payload: CheckOrderIn = Body(default_factory=CheckOrderIn),
    current_user=Depends(get_current_user),
):
    db = get_mongo_db()
    if db is None:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="MongoDB is not configured.")

    order_id = payload.order_id.strip() if payload.order_id and payload.order_id.strip() else None
    payment = None
    if order_id:
        payment = db.payments.find_one({"order_id": order_id, "user_id": current_user.id})
    else:
        # Recovery path for an Android callback that was interrupted after a
        # completed payment. This intentionally scopes the lookup to the user.
        payment = db.payments.find_one(
            {"user_id": current_user.id, "status": {"$in": ["pending", "failed"]}},
            sort=[("created_at", -1)],
        )
        order_id = payment.get("order_id") if payment else None

    if not payment or not order_id:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No payment order found for this account.")

    result = await gateway().check_order_status(order_id)
    if not result.success:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=result.message)

    now = _now()
    db.payments.update_one(
        {"id": payment["id"], "user_id": current_user.id},
        {"$set": {"status": "captured", "payment_id": result.payment_id, "updated_at": now}},
    )
    if payment.get("subscription_id"):
        db.subscriptions.update_one(
            {"id": payment["subscription_id"], "user_id": current_user.id},
            {"$set": {"status": "active", "is_active": True, "start_date": now, "end_date": now + timedelta(days=30), "updated_at": now}},
        )
    return _build_status(current_user)


@router.post(
    "/webhook",
    status_code=status.HTTP_200_OK,
    summary="Payment gateway webhook",
)
async def payment_webhook(request: Request):
    body = await request.body()
    headers = dict(request.headers)
    gw = gateway()
    if not gw.verify_webhook_signature(body, headers):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Webhook signature validation failed.")

    try:
        data = json.loads(body)
    except json.JSONDecodeError:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid JSON payload in webhook body.")

    event = data.get("event", "")
    payment_entity = data.get("payload", {}).get("payment", {}).get("entity", {})
    order_id = payment_entity.get("order_id", data.get("order_id", ""))
    payment_id = payment_entity.get("id", data.get("payment_id", ""))

    logger.info("Webhook received | event=%s | order=%s", event, order_id)
    db = get_mongo_db()
    if db is not None and event in ("payment.captured", "PAYMENT_SUCCESS") and order_id:
        payment = db.payments.find_one({"order_id": order_id})
        if payment and payment.get("status") == "pending":
            now = _now()
            db.payments.update_one({"id": payment["id"]}, {"$set": {"status": "captured", "payment_id": payment_id, "updated_at": now}})
            if payment.get("subscription_id"):
                db.subscriptions.update_one({"id": payment["subscription_id"]}, {"$set": {"status": "active", "is_active": True, "start_date": now, "end_date": now + timedelta(days=30), "updated_at": now}})
    return {"status": "ok"}


@router.get(
    "/status",
    response_model=SubscriptionStatusOut,
    summary="Get subscription status and quota",
)
async def subscription_status(current_user=Depends(get_optional_user)):
    if not current_user:
        return SubscriptionStatusOut(
            has_subscription=False,
            is_active=False,
            days_remaining=0,
            requests_used=0,
            requests_limit=settings.FREE_REQUESTS_LIMIT,
            requests_remaining=settings.FREE_REQUESTS_LIMIT,
            quota_exceeded=False,
        )
    return _build_status(current_user)


def _build_status(user) -> SubscriptionStatusOut:
    db = get_mongo_db()
    sub = None
    if db is not None:
        sub = db.subscriptions.find_one({"user_id": user.id, "status": {"$in": ["active", "pending"]}}, sort=[("created_at", -1)])

    requests_used = get_user_request_count(getattr(user, "id", None), email=getattr(user, "email", None))
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

    now = _now()
    start_date = _as_utc(sub.get("start_date"))
    end_date = _as_utc(sub.get("end_date"))
    is_active = bool(sub.get("is_active")) and end_date is not None and end_date > now
    status_value = sub.get("status")
    if status_value == "active" and not is_active:
        status_value = "expired"
        if db is not None:
            db.subscriptions.update_one({"id": sub["id"]}, {"$set": {"status": "expired", "updated_at": now}})

    return SubscriptionStatusOut(
        has_subscription=True,
        plan=sub.get("plan"),
        status=status_value,
        is_active=is_active,
        starts_at=start_date,
        expires_at=end_date,
        days_remaining=max(0, (end_date - now).days) if end_date else 0,
        requests_used=requests_used,
        requests_limit=requests_limit,
        requests_remaining=requests_remaining,
        quota_exceeded=(requests_used >= requests_limit) and not is_active,
    )
