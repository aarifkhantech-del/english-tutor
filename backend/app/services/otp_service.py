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
import logging
import secrets
from datetime import datetime, timedelta, timezone

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.core.config import settings
from app.models.db_models import OTPRequest, User
from app.services.email_service import send_otp_email

logger = logging.getLogger("english_tutor.otp")


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _get_or_create_user(db: Session, email: str) -> tuple[User, bool]:
    """Return (user, is_new) — creating the user record if this is their first login."""
    user = db.query(User).filter(User.email == email).first()
    if user:
        return user, False
    user = User(email=email)
    db.add(user)
    db.commit()
    db.refresh(user)
    logger.info("New user registered: %s", email)
    return user, True


def _check_rate_limit(db: Session, user_id: str) -> None:
    """Raise 429 if too many OTP requests within the rate-limit window."""
    window_start = _now() - timedelta(minutes=settings.OTP_RATE_LIMIT_WINDOW_MINUTES)
    recent_count = (
        db.query(OTPRequest)
        .filter(
            OTPRequest.user_id == user_id,
            OTPRequest.created_at >= window_start,
        )
        .count()
    )
    if recent_count >= settings.OTP_RATE_LIMIT_COUNT:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=(
                f"Too many OTP requests. Please wait "
                f"{settings.OTP_RATE_LIMIT_WINDOW_MINUTES} minutes before requesting again."
            ),
        )


async def request_otp(db: Session, email: str) -> None:
    """Create a new OTP for the user and send it via email."""
    user, _ = _get_or_create_user(db, email)
    _check_rate_limit(db, user.id)

    # Invalidate any previously unused, unexpired OTPs for this user
    db.query(OTPRequest).filter(
        OTPRequest.user_id == user.id,
        OTPRequest.is_used == False,
    ).update({"is_used": True})

    plain_otp = str(secrets.randbelow(900000) + 100000)  # always 6 digits
    expires_at = _now() + timedelta(minutes=settings.OTP_EXPIRY_MINUTES)

    otp_record = OTPRequest(
        user_id=user.id,
        code_hash=OTPRequest.hash_code(plain_otp),
        expires_at=expires_at,
    )
    db.add(otp_record)
    db.commit()

    logger.info("OTP generated for %s (expires at %s)", email, expires_at.isoformat())
    await send_otp_email(email, plain_otp)


def verify_otp(db: Session, email: str, plain_otp: str) -> tuple[User, bool]:
    """
    Verify the submitted OTP.

    Returns
    -------
    (user, is_new_user) on success.
    Raises HTTPException on any failure (expired, wrong code, too many attempts).
    """
    user = db.query(User).filter(User.email == email).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No account found for this email. Please request an OTP first.",
        )

    # Fetch the latest unused OTP for this user
    otp_record: OTPRequest | None = (
        db.query(OTPRequest)
        .filter(
            OTPRequest.user_id == user.id,
            OTPRequest.is_used == False,
        )
        .order_by(OTPRequest.created_at.desc())
        .first()
    )

    if not otp_record:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No active OTP found. Please request a new one.",
        )

    # Check expiry
    exp = otp_record.expires_at
    if exp.tzinfo is None:
        exp = exp.replace(tzinfo=timezone.utc)
    if _now() > exp:
        otp_record.is_used = True
        db.commit()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="OTP has expired. Please request a new one.",
        )

    # Increment attempt counter before checking (prevents timing attacks)
    otp_record.attempts += 1
    if otp_record.attempts > settings.OTP_MAX_ATTEMPTS:
        otp_record.is_used = True
        db.commit()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Too many incorrect attempts. Please request a new OTP.",
        )
    db.commit()

    if not otp_record.verify(plain_otp):
        remaining = settings.OTP_MAX_ATTEMPTS - otp_record.attempts
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Incorrect OTP. {remaining} attempt(s) remaining.",
        )

    # Mark as used + update last login timestamp
    otp_record.is_used = True
    was_new = user.last_login_at is None
    user.last_login_at = _now()
    db.commit()

    logger.info("OTP verified successfully for %s", email)
    return user, was_new
