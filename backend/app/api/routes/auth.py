from __future__ import annotations
"""
Auth API routes — email OTP login system.

Endpoints
---------
POST /auth/request-otp   — send a 6-digit OTP to the user's email
POST /auth/verify-otp    — verify the OTP and receive a JWT access token
GET  /auth/me            — return the currently authenticated user's profile
"""
import logging

from fastapi import APIRouter, Depends, HTTPException, status
from google.auth.transport import requests as google_requests
from google.oauth2 import id_token as google_id_token

from app.core.config import settings
from app.core.security import create_access_token, get_current_user
from app.models.schemas import (
    GoogleSignInConfigOut,
    GoogleSignInIn,
    OTPRequestIn,
    OTPRequestOut,
    OTPVerifyIn,
    TokenOut,
    UserOut,
)
from app.services import otp_service
from app.services.mongo_repositories import get_or_create_user

logger = logging.getLogger("english_tutor.auth")
router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.get("/google-config", response_model=GoogleSignInConfigOut, include_in_schema=False)
async def google_sign_in_config():
    """Expose only the public OAuth client ID required by Google Identity Services."""
    return GoogleSignInConfigOut(
        enabled=settings.google_sign_in_enabled,
        client_id=settings.GOOGLE_OAUTH_CLIENT_IDS[0] if settings.google_sign_in_enabled else None,
    )


@router.post("/google", response_model=TokenOut, summary="Sign in with Google")
async def sign_in_with_google(payload: GoogleSignInIn):
    """Verify a Google ID token server-side and exchange it for a VocalBharat JWT."""
    if not settings.google_sign_in_enabled:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Google Sign-In is not configured.")

    try:
        claims = google_id_token.verify_oauth2_token(payload.id_token, google_requests.Request())
    except Exception:
        logger.warning("Rejected invalid Google ID token")
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid Google sign-in token.")

    if claims.get("aud") not in settings.GOOGLE_OAUTH_CLIENT_IDS:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Google token was issued for a different client.")
    if claims.get("iss") not in {"accounts.google.com", "https://accounts.google.com"}:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid Google token issuer.")
    if not claims.get("email_verified") or not claims.get("email"):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Your Google account must have a verified email address.")

    email = str(claims["email"]).lower().strip()
    try:
        _, is_new = get_or_create_user(
            email,
            first_name=str(claims.get("given_name") or ""),
            last_name=str(claims.get("family_name") or ""),
            full_name=str(claims.get("name") or ""),
            avatar_url=str(claims.get("picture") or ""),
        )
    except RuntimeError as exc:
        logger.error("Google Sign-In unavailable: %s", exc)
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Authentication is temporarily unavailable.")

    return TokenOut(access_token=create_access_token(email), email=email, is_new_user=is_new)


@router.post(
    "/request-otp",
    response_model=OTPRequestOut,
    status_code=status.HTTP_200_OK,
    summary="Request a login OTP",
    description=(
        "Sends a 6-digit one-time password to the provided email address. "
        "The OTP is valid for 10 minutes. Rate-limited to 3 requests per hour per email."
    ),
)
async def request_otp(payload: OTPRequestIn):
    email = payload.email.lower().strip()
    await otp_service.request_otp(None, email)
    return OTPRequestOut(
        message="OTP sent successfully. Please check your email inbox.",
        email=email,
    )


@router.post(
    "/verify-otp",
    response_model=TokenOut,
    status_code=status.HTTP_200_OK,
    summary="Verify OTP and receive JWT",
    description=(
        "Verifies the 6-digit OTP. On success, returns a JWT bearer token "
        "that must be included in the Authorization header for protected endpoints."
    ),
)
async def verify_otp(payload: OTPVerifyIn):
    email = payload.email.lower().strip()
    user, is_new = otp_service.verify_otp(None, email, payload.otp)
    token = create_access_token(email)
    return TokenOut(
        access_token=token,
        email=email,
        is_new_user=is_new,
    )


@router.get(
    "/me",
    response_model=UserOut,
    status_code=status.HTTP_200_OK,
    summary="Get current user profile",
    description="Returns the authenticated user's profile. Requires a valid JWT bearer token.",
)
async def get_me(current_user=Depends(get_current_user)):
    return UserOut(
        id=current_user.id,
        email=current_user.email,
        first_name=current_user.first_name,
        last_name=current_user.last_name,
        full_name=current_user.full_name,
        avatar_url=current_user.avatar_url,
        is_active=current_user.is_active,
        created_at=current_user.created_at,
        last_login_at=current_user.last_login_at,
    )
