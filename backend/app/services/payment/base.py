"""
Abstract base class for all payment gateway adapters.

To add a new gateway:
1. Create a new file in this package (e.g. cashfree.py)
2. Subclass PaymentGateway and implement all abstract methods
3. Register it in factory.py
"""
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Optional


@dataclass
class OrderResult:
    """Result of creating a payment order."""
    order_id: str           # Gateway's order identifier
    amount: int             # Amount in INR
    currency: str
    gateway_key: Optional[str] = None   # Public key to pass to frontend SDK


@dataclass
class VerifyResult:
    """Result of verifying a payment after the user completes checkout."""
    success: bool
    payment_id: str
    order_id: str
    message: str = ""


class PaymentGateway(ABC):
    """Contract that every payment gateway adapter must fulfil."""

    @abstractmethod
    async def create_order(self, amount: int, currency: str, plan: str) -> OrderResult:
        """
        Create a payment order and return the order details needed by the frontend.

        Parameters
        ----------
        amount   : amount in the smallest currency unit (e.g. paise for INR)
        currency : ISO 4217 currency code, e.g. "INR"
        plan     : subscription plan name ("trial" or "monthly")
        """

    @abstractmethod
    async def verify_payment(
        self,
        order_id: str,
        payment_id: str,
        signature: Optional[str],
    ) -> VerifyResult:
        """
        Verify that a payment was genuinely completed by the gateway.

        For Razorpay: validates HMAC-SHA256 signature.
        For mock: always succeeds.
        """

    @abstractmethod
    async def check_order_status(self, order_id: str) -> VerifyResult:
        """
        Directly query gateway API to check if an order has been paid.
        Useful when webhook or JS callback is delayed/missed (e.g. UPI QR scan).
        """

    @abstractmethod
    def verify_webhook_signature(self, body: bytes, headers: dict) -> bool:
        """
        Validate that an incoming webhook request genuinely came from this gateway.
        Returns True if the signature is valid.
        """

