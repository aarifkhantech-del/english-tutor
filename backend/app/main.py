import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.api.routes import health_router, transcribe_router, tutor_router
from app.services.asr_service import asr_service
from app.services.llm_service import llm_service

# Configure structured logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-7s | %(name)s | %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("english_tutor")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan for pre-warming models and managing connections."""
    logger.info("Initializing Hindi -> English Tutor API v%s...", settings.VERSION)

    # Preload Whisper ASR model on startup so the first request is instant
    asr_service.initialize()

    logger.info("========================================")
    logger.info("Hindi -> English Tutor API is LIVE")
    logger.info("========================================")
    logger.info("API Docs: http://%s:%d/docs", settings.HOST, settings.PORT)
    logger.info("Pipeline: 100%% In-Memory / Zero Disk I/O")
    logger.info("========================================")

    yield

    # Cleanup resources on shutdown
    logger.info("Shutting down API resources...")
    await llm_service.close()
    logger.info("Shutdown complete.")


def create_app() -> FastAPI:
    """FastAPI Application Factory."""
    app = FastAPI(
        title=settings.PROJECT_NAME,
        description=settings.DESCRIPTION,
        version=settings.VERSION,
        lifespan=lifespan,
    )

    # CORS configuration
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.CORS_ORIGINS,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # Register API Routers
    app.include_router(health_router)
    app.include_router(transcribe_router)
    app.include_router(tutor_router)

    return app


app = create_app()
