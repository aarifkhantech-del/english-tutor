"""
Razorpay payment gateway adapter.

Setup
-----
1. Set PAYMENT_GATEWAY=razorpay in your .env
2. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET from your Razorpay dashboard
3. Install razorpay: pip install razorpay

All amounts are passed in paise (INR × 100). e.g. ₹5 → 500 paise.

Webhook verification uses the X-Razorpay-Signature header.
"""
import hashlib
import hmac
import logging
from typing import Optional

from app.core.config import settings
from app.services.payment.base import OrderResult, PaymentGateway, VerifyResult

logger = logging.getLogger("english_tutor.payment.razorpay")


class RazorpayGateway(PaymentGateway):
    """
    Production-ready Razorpay adapter.

    Requires: pip install razorpay
    Docs:     https://razorpay.com/docs/payments/server-integration/python/
    """

    def __init__(self) -> None:
        try:
            import razorpay  # type: ignore
            self._client = razorpay.Client(
                auth=(settings.RAZORPAY_KEY_ID, settings.RAZORPAY_KEY_SECRET)
            )
            logger.info("Razorpay gateway initialized with key_id=%s", settings.RAZORPAY_KEY_ID[:8] + "...")
        except ImportError as exc:
            raise ImportError(
                "Razorpay SDK not installed. Run: pip install razorpay"
            ) from exc

    async def create_order(self, amount: int, currency: str, plan: str) -> OrderResult:
        """
        Create a Razorpay order.

        amount is in INR (whole rupees). Razorpay expects paise, so we multiply by 100.
        """
        amount_paise = amount * 100
        data = {
            "amount": amount_paise,
            "currency": currency,
            "notes": {"plan": plan},
        }
        order = self._client.order.create(data=data)
        logger.info("Razorpay order created: %s | ₹%d", order["id"], amount)
        return OrderResult(
            order_id=order["id"],
            amount=amount,
            currency=currency,
            gateway_key=settings.RAZORPAY_KEY_ID,
        )

    async def verify_payment(
        self,
        order_id: str,
        payment_id: str,
        signature: Optional[str],
    ) -> VerifyResult:
        """
        Verify Razorpay payment using HMAC-SHA256 signature.

        The frontend sends back: razorpay_order_id, razorpay_payment_id, razorpay_signature.
        We reconstruct the message and validate the signature against our secret key.
        """
        if not signature:
            return VerifyResult(
                success=False,
                payment_id=payment_id,
                order_id=order_id,
                message="Payment signature is missing.",
            )

        message = f"{order_id}|{payment_id}"
        secret = settings.RAZORPAY_KEY_SECRET.encode()
        expected = hmac.new(secret, message.encode(), hashlib.sha256).hexdigest()
        valid = hmac.compare_digest(expected, signature)

        if valid:
            logger.info("Razorpay payment verified: order=%s payment=%s", order_id, payment_id)
        else:
            logger.warning("Razorpay signature mismatch: order=%s", order_id)

        return VerifyResult(
            success=valid,
            payment_id=payment_id,
            order_id=order_id,
            message="Payment verified." if valid else "Payment signature validation failed.",
        )

    async def check_order_status(self, order_id: str) -> VerifyResult:
        """
        Check order status directly with Razorpay API.
        Useful when frontend webhook/callback is delayed or user pays via UPI QR scan.
        """
        try:
            order = self._client.order.fetch(order_id)
            order_status = order.get("status", "")
            amount_paid = order.get("amount_paid", 0)

            # Check payments linked to this order
            payments_res = self._client.order.payments(order_id)
            items = payments_res.get("items", []) if isinstance(payments_res, dict) else []

            captured_payment = next(
                (p for p in items if p.get("status") in ("captured", "authorized")),
                None
            )

            if order_status == "paid" or amount_paid > 0 or captured_payment:
                pay_id = (captured_payment.get("id") if captured_payment else None) or (items[0].get("id") if items else f"pay_order_{order_id}")
                logger.info("Razorpay order %s is verified as PAID (payment_id=%s)", order_id, pay_id)
                return VerifyResult(
                    success=True,
                    payment_id=pay_id,
                    order_id=order_id,
                    message="Payment verified directly from Razorpay.",
                )

            return VerifyResult(
                success=False,
                payment_id="",
                order_id=order_id,
                message=f"Order status on Razorpay is '{order_status}'. Payment not yet confirmed.",
            )
        except Exception as e:
            logger.error("Error fetching Razorpay order %s: %s", order_id, e)
            return VerifyResult(
                success=False,
                payment_id="",
                order_id=order_id,
                message=f"Could not verify with Razorpay: {str(e)}",
            )

    def verify_webhook_signature(self, body: bytes, headers: dict) -> bool:
        """Validate the X-Razorpay-Signature webhook header."""
        received = headers.get("x-razorpay-signature", "")
        secret = settings.RAZORPAY_KEY_SECRET.encode()
        expected = hmac.new(secret, body, hashlib.sha256).hexdigest()
        valid = hmac.compare_digest(received, expected)
        if not valid:
            logger.warning("Razorpay webhook signature mismatch")
        return valid

