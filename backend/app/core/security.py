"""JWT creation and verification utilities for the VocalBharat auth system."""
import logging
from datetime import datetime, timedelta, timezone

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import JWTError, jwt
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db

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


def get_current_user(
    email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
):
    """FastAPI dependency — returns the User ORM object for the authenticated user."""
    from app.models.db_models import User  # avoid circular import

    user = db.query(User).filter(User.email == email, User.is_active == True).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Account not found or deactivated.",
        )
    return user


def get_optional_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
):
    """FastAPI dependency — returns current User if bearer token provided, else None."""
    if credentials is None:
        return None
    try:
        payload = _decode_token(credentials.credentials)
        email = payload.get("sub")
        if not email:
            return None
        from app.models.db_models import User
        return db.query(User).filter(User.email == email, User.is_active == True).first()
    except Exception:
        return None


def verify_usage_quota(
    user: User | None = Depends(get_optional_user),
    db: Session = Depends(get_db),
):
    """
    Enforces 20-request quota for free tier.
    If user is authenticated and has active subscription -> unlimited access.
    If requests >= FREE_REQUESTS_LIMIT (20) and no active plan -> raises 402.
    """
    if user is None:
        return None

    from app.models.db_models import Subscription

    # Check active subscription
    sub = (
        db.query(Subscription)
        .filter(
            Subscription.user_id == user.id,
            Subscription.status == "active",
        )
        .first()
    )

    if sub and sub.is_active:
        user.request_count = (user.request_count or 0) + 1
        db.commit()
        return user

    requests_used = user.request_count or 0
    if requests_used >= settings.FREE_REQUESTS_LIMIT:
        raise HTTPException(
            status_code=status.HTTP_402_PAYMENT_REQUIRED,
            detail=f"You have used all {settings.FREE_REQUESTS_LIMIT} free practice sessions. Please select a subscription plan to continue.",
        )

    user.request_count = requests_used + 1
    db.commit()
    return user
