from pathlib import Path
from fastapi import APIRouter, Request
from fastapi.responses import FileResponse
from app.models.schemas import HealthResponse
from app.services.asr_service import asr_service
from app.core.config import settings

router = APIRouter(tags=["System"])
STATIC_DIR = Path(__file__).resolve().parent.parent.parent / "static"


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
async def root(request: Request):
    """Serve the web application frontend for web browsers, or JSON for API clients."""
    accept = request.headers.get("accept", "")
    index_file = STATIC_DIR / "index.html"
    if "text/html" in accept and index_file.exists():
        return FileResponse(index_file)
    return {
        "name": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "status": "online",
        "app": "/app",
        "docs": "/docs",
        "checkout": "/checkout",
    }


@router.get("/app", include_in_schema=False)
@router.get("/tutor", include_in_schema=False)
@router.get("/grammar-explorer", include_in_schema=False)
@router.get("/pricing", include_in_schema=False)
@router.get("/checkout", include_in_schema=False)
@router.get("/feedback-form", include_in_schema=False)
@router.get("/help", include_in_schema=False)
async def serve_webapp():
    """Serve the VocalBharat Web Application."""
    index_file = STATIC_DIR / "index.html"
    if index_file.exists():
        return FileResponse(index_file)
    return {"message": "Web application index.html not found"}

