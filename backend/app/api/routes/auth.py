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

from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.security import create_access_token, get_current_user
from app.models.db_models import User
from app.models.schemas import OTPRequestIn, OTPRequestOut, OTPVerifyIn, TokenOut, UserOut
from app.services import otp_service

logger = logging.getLogger("english_tutor.auth")
router = APIRouter(prefix="/auth", tags=["Authentication"])


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
async def request_otp(payload: OTPRequestIn, db: Session = Depends(get_db)):
    email = payload.email.lower().strip()
    await otp_service.request_otp(db, email)
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
async def verify_otp(payload: OTPVerifyIn, db: Session = Depends(get_db)):
    email = payload.email.lower().strip()
    user, is_new = otp_service.verify_otp(db, email, payload.otp)
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
async def get_me(current_user: User = Depends(get_current_user)):
    return current_user
