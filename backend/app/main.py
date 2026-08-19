import logging
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from pymongo.errors import PyMongoError

from app.core.config import settings
from app.api.routes import (
    health_router,
    transcribe_router,
    tutor_router,
    grammar_router,
    auth_router,
    subscription_router,
    payment_router,
    feedback_router,
)
from app.services.asr_service import asr_service
from app.services.llm_service import llm_service
from app.services.grammar_service import grammar_service

STATIC_DIR = Path(__file__).resolve().parent / "static"

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

    # MongoDB is the active persistence layer; SQLAlchemy table bootstrap is intentionally disabled.
    from app.models import db_models  # noqa: F401 — ensures models are registered
    from app.core.database import init_db
    init_db()
    logger.info("Mongo-only persistence mode active.")

    # Initialize MongoDB if configured
    from app.core.mongodb import init_mongodb, close_mongodb
    mongo_ok = init_mongodb()
    if mongo_ok:
        logger.info("MongoDB Atlas persistence is active.")

    # Preload Whisper ASR model on startup so the first request is instant
    asr_service.initialize()

    logger.info("========================================")
    logger.info("Hindi -> English Tutor API is LIVE")
    logger.info("========================================")
    logger.info("API Docs:   http://%s:%d/docs", settings.HOST, settings.PORT)
    logger.info("Auth:       http://%s:%d/auth", settings.HOST, settings.PORT)
    logger.info("Payments:   http://%s:%d/subscription", settings.HOST, settings.PORT)
    logger.info("Storage:    %s", "MongoDB Atlas" if mongo_ok else "SQLite")
    logger.info("Frontend:   http://%s:%d/app", settings.HOST, settings.PORT)
    logger.info("Pipeline:   100%% In-Memory / Zero Disk I/O")
    logger.info("Gateway:    %s", settings.PAYMENT_GATEWAY.upper())
    logger.info("========================================")

    yield

    # Cleanup resources on shutdown
    logger.info("Shutting down API resources...")
    await llm_service.close()
    await grammar_service.close()
    close_mongodb()
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

    # Mount static assets if directory exists
    if STATIC_DIR.exists():
        app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")

    # Register API Routers
    @app.exception_handler(PyMongoError)
    async def mongodb_exception_handler(_request: Request, exc: PyMongoError):
        logger.error("MongoDB error: %s", exc)
        return JSONResponse(
            status_code=503,
            content={
                "detail": (
                    "Database is temporarily unavailable. "
                    "Whitelist this machine's public IP in MongoDB Atlas Network Access."
                )
            },
        )

    app.include_router(health_router)
    app.include_router(transcribe_router)
    app.include_router(tutor_router)
    app.include_router(grammar_router, prefix="/grammar")
    app.include_router(auth_router)
    app.include_router(subscription_router)
    app.include_router(payment_router)
    app.include_router(feedback_router)

    return app


app = create_app()


