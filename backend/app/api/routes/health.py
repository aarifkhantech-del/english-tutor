from fastapi import APIRouter
from app.models.schemas import HealthResponse
from app.services.asr_service import asr_service
from app.core.config import settings

router = APIRouter(tags=["System"])


@router.get("/health", response_model=HealthResponse)
async def health_check() -> HealthResponse:
    """System health check and component status."""
    return HealthResponse(
        status="healthy",
        whisper="loaded" if asr_service.is_loaded else "ready",
        mistral="configured" if settings.MISTRAL_API_KEY else "unconfigured",
        tts="configured",
        version=settings.VERSION,
    )


@router.get("/")
async def root() -> dict[str, str]:
    """Root status indicator."""
    return {
        "name": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "status": "online",
        "docs": "/docs",
    }
