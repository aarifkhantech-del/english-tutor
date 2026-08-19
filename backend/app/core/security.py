from __future__ import annotations
import logging
from datetime import datetime, timedelta, timezone
from typing import Any

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import JWTError, jwt
from pymongo.errors import PyMongoError

from app.core.config import settings
from app.services.mongo_repositories import (
    MongoUser,
    get_active_subscription,
    get_active_subscription_by_email,
    get_user_by_email,
    get_user_request_count,
    increment_user_request_count,
)

logger = logging.getLogger("english_tutor.security")
bearer_scheme = HTTPBearer(auto_error=False)


def create_access_token(email: str) -> str:
    """Create a signed JWT access token valid for JWT_EXPIRY_DAYS days."""
    expire = datetime.now(timezone.utc) + timedelta(days=settings.JWT_EXPIRY_DAYS)
    payload = {"sub": email, "exp": expire, "iat": datetime.now(timezone.utc)}
    return jwt.encode(payload, settings.SECRET_KEY, algorithm=settings.JWT_ALGORITHM)


def _decode_token(token: str) -> dict:
    """Decode and validate a JWT; raises HTTPException on failure."""
    try:
        return jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.JWT_ALGORITHM])
    except JWTError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired authentication token.",
            headers={"WWW-Authenticate": "Bearer"},
        ) from exc


def get_current_user_email(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
) -> str:
    """FastAPI dependency — returns the authenticated user's email or raises 401."""
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication required. Please log in.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    payload = _decode_token(credentials.credentials)
    email: str | None = payload.get("sub")
    if not email:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token payload is missing user identity.",
        )
    return email


def _mongo_unavailable() -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail=(
            "User database is temporarily unavailable. "
            "If you are running locally, whitelist this IP in MongoDB Atlas Network Access."
        ),
    )


def get_current_user(email: str = Depends(get_current_user_email)) -> MongoUser:
    """FastAPI dependency — returns the current authenticated Mongo user."""
    try:
        user_doc = get_user_by_email(email)
    except PyMongoError as exc:
        logger.error("MongoDB error while loading current user: %s", exc)
        raise _mongo_unavailable() from exc
    if not user_doc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Account not found or deactivated.",
        )

    user = MongoUser(user_doc)
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Account not found or deactivated.",
        )
    return user


def get_optional_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
):
    """FastAPI dependency — returns current user if bearer token provided, else None."""
    if credentials is None:
        return None
    try:
        payload = _decode_token(credentials.credentials)
        email = payload.get("sub")
        if not email:
            return None
        user_doc = get_user_by_email(email)
        if not user_doc:
            return None
        user = MongoUser(user_doc)
        return user if user.is_active else None
    except Exception:
        return None


def verify_usage_quota(user: Any | None = Depends(get_optional_user)):
    """
    Enforces the free-tier quota for the Mongo-backed app.
    If the user has an active subscription, request count is still incremented and access continues.
    """
    if user is None:
        return None

    sub = get_active_subscription(user.id)
    if sub and sub.get("is_active"):
        user.request_count = increment_user_request_count(user.id, email=user.email)
        return user

    requests_used = get_user_request_count(user.id, email=user.email)
    if requests_used >= settings.FREE_REQUESTS_LIMIT:
        raise HTTPException(
            status_code=status.HTTP_402_PAYMENT_REQUIRED,
            detail=f"You have used all {settings.FREE_REQUESTS_LIMIT} free practice sessions. Please select a subscription plan to continue.",
        )

    user.request_count = increment_user_request_count(user.id, email=user.email)
    return user


def require_active_subscription(user: Any | None = Depends(get_optional_user)):
    """Grammar Explorer and other Pro-only features require an active paid plan."""
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Please sign in and subscribe to Monthly Pro to use Grammar Explorer.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    sub = get_active_subscription(user.id) or get_active_subscription_by_email(user.email)
    if not sub:
        raise HTTPException(
            status_code=status.HTTP_402_PAYMENT_REQUIRED,
            detail="Grammar Explorer is a Monthly Pro feature. Please subscribe to continue.",
        )
    return user


def quota_payload(user: Any | None) -> dict:
    """Usage fields to include on tutor/grammar responses."""
    limit = settings.FREE_REQUESTS_LIMIT
    used = int(getattr(user, "request_count", 0) or 0) if user is not None else 0
    return {
        "requests_used": used,
        "requests_limit": limit,
        "requests_remaining": max(0, limit - used),
        "quota_exceeded": used >= limit,
    }
