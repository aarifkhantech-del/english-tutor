"""
Payment gateway factory.

Returns the correct gateway instance based on the PAYMENT_GATEWAY environment variable.

Supported values
----------------
  mock      — local development (default)
  razorpay  — set RAZORPAY_KEY_ID + RAZORPAY_KEY_SECRET
  stripe    — set STRIPE_SECRET_KEY (stub — extend StripeGateway when needed)
  cashfree  — coming soon (follow the base.py contract to implement)
"""
import logging

from app.core.config import settings
from app.services.payment.base import PaymentGateway

logger = logging.getLogger("english_tutor.payment.factory")


def get_payment_gateway() -> PaymentGateway:
    """Return the active payment gateway adapter based on PAYMENT_GATEWAY setting."""
    gw = settings.PAYMENT_GATEWAY.lower()

    if gw == "mock":
        from app.services.payment.mock import MockGateway
        logger.info("Payment gateway: Mock (development mode)")
        return MockGateway()

    if gw == "razorpay":
        from app.services.payment.razorpay import RazorpayGateway
        logger.info("Payment gateway: Razorpay")
        return RazorpayGateway()

    raise ValueError(
        f"Unsupported PAYMENT_GATEWAY='{gw}'. "
        "Supported values: mock, razorpay. "
        "Add a new adapter in app/services/payment/ to support others."
    )


# Cached singleton — instantiated once on first import
_gateway_instance: PaymentGateway | None = None


def gateway() -> PaymentGateway:
    """Cached gateway singleton for use as a FastAPI dependency."""
    global _gateway_instance
    if _gateway_instance is None:
        _gateway_instance = get_payment_gateway()
    return _gateway_instance
