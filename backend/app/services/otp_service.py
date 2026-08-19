"""
OTP generation, storage, and verification service.

Security model
--------------
- OTPs are 6-digit random codes (cryptographically secure via secrets module)
- Only the SHA-256 hash is stored in the database — never the raw code
- Each OTP expires after OTP_EXPIRY_MINUTES (default: 10 min)
- Max OTP_MAX_ATTEMPTS (default: 5) wrong guesses before the OTP is invalidated
- Rate limit: max OTP_RATE_LIMIT_COUNT requests per OTP_RATE_LIMIT_WINDOW_MINUTES per email
"""
import hashlib
import logging
import secrets
from datetime import datetime, timedelta, timezone
from typing import Any

from fastapi import HTTPException, status
from pymongo.errors import PyMongoError

from app.core.config import settings
from app.core.mongodb import get_mongo_db
from app.services.email_service import send_otp_email
from app.services.mongo_repositories import (
    count_recent_otps,
    create_otp_record,
    get_active_otp,
    get_or_create_user,
    get_user_by_email,
    increment_otp_attempts,
    invalidate_pending_otps,
    mark_otp_used,
)

logger = logging.getLogger("english_tutor.otp")


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _hash_otp(plain_otp: str) -> str:
    return hashlib.sha256(plain_otp.encode("utf-8")).hexdigest()


def _get_or_create_user(email: str) -> tuple[dict[str, Any], bool]:
    """Return (user_doc, is_new) using the Mongo repository."""
    user_doc, is_new = get_or_create_user(email)
    logger.info("User registered via Mongo: %s (new=%s)", email, is_new)
    return user_doc, is_new


def _check_rate_limit(user_id: str) -> None:
    """Raise 429 if too many OTP requests within the rate-limit window."""
    recent_count = count_recent_otps(user_id, settings.OTP_RATE_LIMIT_WINDOW_MINUTES)
    if recent_count >= settings.OTP_RATE_LIMIT_COUNT:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=(
                f"Too many OTP requests. Please wait "
                f"{settings.OTP_RATE_LIMIT_WINDOW_MINUTES} minutes before requesting again."
            ),
        )


def _raise_mongo_unavailable(exc: Exception) -> None:
    logger.error("MongoDB unavailable during OTP flow: %s", exc)
    raise HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail=(
            "Login is temporarily unavailable because the database could not be reached. "
            "Whitelist this machine's public IP in MongoDB Atlas Network Access, then retry."
        ),
    ) from exc


async def request_otp(db: Any, email: str) -> None:
    """Create a new OTP for the user and send it via email."""
    try:
        user_doc, _ = _get_or_create_user(email)
        _check_rate_limit(user_doc["id"])
        invalidate_pending_otps(user_doc["id"])

        plain_otp = str(secrets.randbelow(900000) + 100000)  # always 6 digits
        expires_at = _now() + timedelta(minutes=settings.OTP_EXPIRY_MINUTES)
        create_otp_record(user_doc["id"], email, _hash_otp(plain_otp), expires_at)
    except HTTPException:
        raise
    except (PyMongoError, RuntimeError) as exc:
        _raise_mongo_unavailable(exc)

    logger.info("OTP generated for %s (expires at %s)", email, expires_at.isoformat())
    await send_otp_email(email, plain_otp)


def verify_otp(db: Any, email: str, plain_otp: str) -> tuple[dict[str, Any], bool]:
    """
    Verify the submitted OTP against MongoDB-backed records.
    """
    try:
        return _verify_otp(email, plain_otp)
    except HTTPException:
        raise
    except (PyMongoError, RuntimeError) as exc:
        _raise_mongo_unavailable(exc)


def _verify_otp(email: str, plain_otp: str) -> tuple[dict[str, Any], bool]:
    user = get_user_by_email(email)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No account found for this email. Please request an OTP first.",
        )

    otp_record = get_active_otp(user["id"])
    if not otp_record:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No active OTP found. Please request a new one.",
        )

    exp = otp_record["expires_at"]
    if exp.tzinfo is None:
        exp = exp.replace(tzinfo=timezone.utc)
    if _now() > exp:
        mark_otp_used(otp_record["id"])
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="OTP has expired. Please request a new one.",
        )

    attempts = increment_otp_attempts(otp_record["id"])
    if attempts > settings.OTP_MAX_ATTEMPTS:
        mark_otp_used(otp_record["id"])
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Too many incorrect attempts. Please request a new OTP.",
        )

    if otp_record["otp_hash"] != _hash_otp(plain_otp):
        remaining = settings.OTP_MAX_ATTEMPTS - attempts
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Incorrect OTP. {remaining} attempt(s) remaining.",
        )

    mark_otp_used(otp_record["id"])
    was_new = user.get("last_login_at") is None
    mongo_db = get_mongo_db()
    if mongo_db is not None:
        mongo_db.users.update_one(
            {"id": user["id"]},
            {"$set": {"last_login_at": _now(), "updated_at": _now()}},
        )

    logger.info("OTP verified successfully for %s", email)
    return user, was_new
