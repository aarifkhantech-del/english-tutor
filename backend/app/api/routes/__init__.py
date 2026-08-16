from .health import router as health_router
from .transcribe import router as transcribe_router
from .tutor import router as tutor_router
from .grammar import router as grammar_router
from .auth import router as auth_router
from .subscription import router as subscription_router
from .payment import router as payment_router
from .feedback import router as feedback_router

__all__ = [
    "health_router",
    "transcribe_router",
    "tutor_router",
    "grammar_router",
    "auth_router",
    "subscription_router",
    "payment_router",
    "feedback_router",
]


