from .health import router as health_router
from .transcribe import router as transcribe_router
from .tutor import router as tutor_router

__all__ = ["health_router", "transcribe_router", "tutor_router"]
