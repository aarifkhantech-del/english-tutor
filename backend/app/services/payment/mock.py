"""
Mock payment gateway for local development and testing.

Behaviour
---------
- create_order  : returns a deterministic fake order ID instantly
- verify_payment: always succeeds (simulates a successful payment)
- webhook       : validates against MOCK_WEBHOOK_SECRET header

No real money is moved. Switch to a real gateway by changing PAYMENT_GATEWAY in .env.
"""
import hashlib
import hmac
import logging
import uuid
from typing import Optional

from app.core.config import settings
from app.services.payment.base import OrderResult, PaymentGateway, VerifyResult

logger = logging.getLogger("english_tutor.payment.mock")


class MockGateway(PaymentGateway):
    """Development-only gateway that simulates a successful payment flow."""

    async def create_order(self, amount: int, currency: str, plan: str) -> OrderResult:
        order_id = f"mock_order_{uuid.uuid4().hex[:16]}"
        logger.info("[MOCK] Created order %s | amount=%d %s | plan=%s", order_id, amount, currency, plan)
        return OrderResult(
            order_id=order_id,
            amount=amount,
            currency=currency,
            gateway_key="mock_public_key",
        )

    async def verify_payment(
        self,
        order_id: str,
        payment_id: str,
        signature: Optional[str],
    ) -> VerifyResult:
        logger.info("[MOCK] Payment verified | order=%s payment=%s", order_id, payment_id)
        return VerifyResult(
            success=True,
            payment_id=payment_id or f"mock_pay_{uuid.uuid4().hex[:16]}",
            order_id=order_id,
            message="Mock payment verified successfully.",
        )

    def verify_webhook_signature(self, body: bytes, headers: dict) -> bool:
        secret = settings.MOCK_WEBHOOK_SECRET.encode()
        received = headers.get("x-mock-signature", "")
        expected = hmac.new(secret, body, hashlib.sha256).hexdigest()
        valid = hmac.compare_digest(received, expected)
        if not valid:
            logger.warning("[MOCK] Webhook signature mismatch")
        return valid
