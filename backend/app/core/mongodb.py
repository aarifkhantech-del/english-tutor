"""
MongoDB connection manager and initialization for VocalBharat.

Provides singleton database client and auto-initializes indexes for:
- users (email unique)
- otp_requests (user_id, email, created_at)
- subscriptions (user_id, email, is_active)
- payments (user_id, order_id)
"""
import logging
from typing import Optional

import certifi
from pymongo import MongoClient, ASCENDING, DESCENDING
from pymongo.database import Database
from pymongo.errors import PyMongoError

from app.core.config import settings

logger = logging.getLogger("english_tutor.mongodb")

_client: Optional[MongoClient] = None
_db: Optional[Database] = None


def _client_kwargs() -> dict:
    """TLS options that avoid common Atlas handshake failures on Windows/Python."""
    return {
        "serverSelectionTimeoutMS": 8000,
        "connectTimeoutMS": 8000,
        "socketTimeoutMS": 8000,
        "tls": True,
        "tlsCAFile": certifi.where(),
        "tlsDisableOCSPEndpointCheck": True,
    }


def get_mongo_client() -> Optional[MongoClient]:
    global _client
    if _client is None and settings.use_mongodb:
        try:
            _client = MongoClient(settings.MONGODB_URI, **_client_kwargs())
        except Exception as exc:
            logger.error("Failed to initialize MongoClient: %s", exc)
            return None
    return _client


def get_mongo_db() -> Optional[Database]:
    global _db
    if _db is None:
        client = get_mongo_client()
        if client:
            _db = client[settings.MONGODB_DB_NAME]
    return _db


def init_mongodb() -> bool:
    """Check connectivity and initialize indexes. Returns True on success."""
    if not settings.use_mongodb:
        logger.info("MONGODB_URI not configured. MongoDB persistence is disabled.")
        return False

    try:
        db = get_mongo_db()
        if db is None:
            logger.warning("MongoDB client unavailable.")
            return False

        db.command("ping")
        logger.info("Connected successfully to MongoDB Atlas database '%s'", settings.MONGODB_DB_NAME)

        db.users.create_index([("email", ASCENDING)], unique=True, background=True)
        db.otp_requests.create_index([("email", ASCENDING), ("created_at", DESCENDING)], background=True)
        db.otp_requests.create_index([("user_id", ASCENDING)], background=True)
        db.subscriptions.create_index([("user_id", ASCENDING)], background=True)
        db.subscriptions.create_index([("email", ASCENDING)], background=True)
        db.subscriptions.create_index([("is_active", ASCENDING)], background=True)
        db.payments.create_index([("user_id", ASCENDING)], background=True)
        db.payments.create_index([("order_id", ASCENDING)], background=True)

        logger.info("MongoDB indexes verified for [users, otp_requests, subscriptions, payments].")
        return True
    except PyMongoError as exc:
        logger.error(
            "MongoDB Atlas connection failed: %s. "
            "If this is TLSV1_ALERT_INTERNAL_ERROR, whitelist this machine's public IP "
            "in Atlas Network Access (or allow 0.0.0.0/0 for local development).",
            exc,
        )
        return False
    except Exception as exc:
        logger.error("MongoDB Atlas connection failed: %s", exc)
        return False


def close_mongodb():
    global _client, _db
    if _client:
        try:
            _client.close()
            logger.info("MongoDB client disconnected.")
        except Exception:
            pass
        _client = None
        _db = None
