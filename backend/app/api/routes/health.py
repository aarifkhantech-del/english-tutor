from pathlib import Path
from fastapi import APIRouter, Request
from fastapi.responses import FileResponse
from app.models.schemas import HealthResponse
from app.services.asr_service import asr_service
from app.core.config import settings

router = APIRouter(tags=["System"])
STATIC_DIR = Path(__file__).resolve().parent.parent.parent / "static"


@router.get("/robots.txt", include_in_schema=False)
async def robots_txt():
    robots = STATIC_DIR / "robots.txt"
    if robots.exists():
        return FileResponse(robots, media_type="text/plain")
    return {"detail": "not found"}


@router.get("/sitemap.xml", include_in_schema=False)
async def sitemap_xml():
    sitemap = STATIC_DIR / "sitemap.xml"
    if sitemap.exists():
        return FileResponse(sitemap, media_type="application/xml")
    return {"detail": "not found"}


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
        "privacy": "/privacy",
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


@router.get("/privacy", include_in_schema=False)
@router.get("/privacy-policy", include_in_schema=False)
async def serve_privacy_policy():
    """Public privacy policy required for Play Console and the web app."""
    policy = STATIC_DIR / "privacy-policy.html"
    if policy.exists():
        return FileResponse(policy, media_type="text/html")
    return {"detail": "Privacy policy not found"}

