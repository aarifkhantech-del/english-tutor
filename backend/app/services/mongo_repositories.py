"""
MongoDB Repositories for VocalBharat user, OTP, subscription, and payment entities.
"""
import uuid
from datetime import datetime, timedelta, timezone
from typing import Optional, Tuple, List, Dict, Any
from pymongo import ReturnDocument
from app.core.config import settings
from app.core.mongodb import get_mongo_db


def _now() -> datetime:
    return datetime.now(timezone.utc)


class MongoUser:
    """Wrapper to provide both attribute and dictionary access for User documents."""
    def __init__(self, data: Dict[str, Any]):
        self._data = data
        self.id = data.get("id") or str(data.get("_id", ""))
        self.email = data.get("email", "")
        self.first_name = data.get("first_name", "")
        self.last_name = data.get("last_name", "")
        self.full_name = data.get("full_name", "")
        self.avatar_url = data.get("avatar_url", "")
        self.is_active = data.get("is_active", True)
        self.request_count = int(data.get("request_count", 0))
        self.created_at = data.get("created_at")
        self.last_login_at = data.get("last_login_at")

    def __getitem__(self, key):
        return self._data[key]

    def get(self, key, default=None):
        return self._data.get(key, default)

    def __repr__(self) -> str:
        return f"<MongoUser email={self.email} id={self.id} requests={self.request_count}>"


# ── Users ────────────────────────────────────────────────────────────────────

def get_user_by_email(email: str) -> Optional[Dict[str, Any]]:
    db = get_mongo_db()
    if db is None:
        return None
    return db.users.find_one({"email": email.lower().strip()})


def get_user_by_id(user_id: str) -> Optional[Dict[str, Any]]:
    db = get_mongo_db()
    if db is None:
        return None
    return db.users.find_one({"id": user_id})


def get_or_create_user(
    email: str,
    first_name: str = "",
    last_name: str = "",
    full_name: str = "",
    avatar_url: str = "",
) -> Tuple[Dict[str, Any], bool]:
    db = get_mongo_db()
    if db is None:
        raise RuntimeError("MongoDB is not configured or the client failed to initialize.")
    clean_email = email.lower().strip()
    existing = db.users.find_one({"email": clean_email})
    if existing:
        profile_updates = {
            key: value for key, value in {
                "first_name": first_name.strip(),
                "last_name": last_name.strip(),
                "full_name": full_name.strip(),
                "avatar_url": avatar_url.strip(),
            }.items() if value
        }
        if profile_updates:
            profile_updates["updated_at"] = _now()
            existing = db.users.find_one_and_update(
                {"id": existing["id"]},
                {"$set": profile_updates},
                return_document=ReturnDocument.AFTER,
            )
        return existing, False

    user_doc = {
        "id": str(uuid.uuid4()),
        "email": clean_email,
        "first_name": first_name.strip(),
        "last_name": last_name.strip(),
        "full_name": full_name.strip(),
        "avatar_url": avatar_url.strip(),
        "is_active": True,
        "request_count": 0,
        "created_at": _now(),
        "updated_at": _now(),
    }
    db.users.insert_one(user_doc)
    return user_doc, True


def increment_user_request_count(user_id: Optional[str] = None, email: Optional[str] = None) -> int:
    """Atomically increment user's total request count and return updated count."""
    db = get_mongo_db()
    if db is None:
        return 0
    update = {"$inc": {"request_count": 1}, "$set": {"updated_at": _now()}}
    res = None
    if user_id:
        res = db.users.find_one_and_update(
            {"id": user_id},
            update,
            return_document=ReturnDocument.AFTER,
        )
    if res is None and email:
        res = db.users.find_one_and_update(
            {"email": email.lower().strip()},
            update,
            return_document=ReturnDocument.AFTER,
        )
    return int(res.get("request_count", 0)) if res else 0


def get_user_request_count(user_id: Optional[str] = None, email: Optional[str] = None) -> int:
    user = get_user_by_id(user_id) if user_id else None
    if user is None and email:
        user = get_user_by_email(email)
    return int(user.get("request_count", 0)) if user else 0


# ── OTP Requests ─────────────────────────────────────────────────────────────

def count_recent_otps(user_id: str, minutes: int) -> int:
    db = get_mongo_db()
    if db is None:
        return 0
    window_start = _now() - timedelta(minutes=minutes)
    return db.otp_requests.count_documents({
        "user_id": user_id,
        "created_at": {"$gte": window_start},
    })


def invalidate_pending_otps(user_id: str) -> None:
    db = get_mongo_db()
    if db is None:
        return
    db.otp_requests.update_many(
        {
            "user_id": user_id,
            "is_used": False,
            "expires_at": {"$gt": _now()},
        },
        {"$set": {"is_used": True}}
    )


def create_otp_record(user_id: str, email: str, otp_hash: str, expires_at: datetime) -> Dict[str, Any]:
    db = get_mongo_db()
    doc = {
        "id": str(uuid.uuid4()),
        "user_id": user_id,
        "email": email.lower().strip(),
        "otp_hash": otp_hash,
        "attempts": 0,
        "is_used": False,
        "expires_at": expires_at,
        "created_at": _now(),
    }
    db.otp_requests.insert_one(doc)
    return doc


def get_active_otp(user_id: str) -> Optional[Dict[str, Any]]:
    db = get_mongo_db()
    if db is None:
        return None
    return db.otp_requests.find_one(
        {
            "user_id": user_id,
            "is_used": False,
            "expires_at": {"$gt": _now()},
        },
        sort=[("created_at", -1)]
    )


def increment_otp_attempts(otp_id: str) -> int:
    db = get_mongo_db()
    if db is None:
        return 0
    res = db.otp_requests.find_one_and_update(
        {"id": otp_id},
        {"$inc": {"attempts": 1}},
        return_document=True
    )
    return res.get("attempts", 0) if res else 0


def mark_otp_used(otp_id: str) -> None:
    db = get_mongo_db()
    if db is None:
        return
    db.otp_requests.update_one(
        {"id": otp_id},
        {"$set": {"is_used": True}}
    )


# ── Subscriptions ────────────────────────────────────────────────────────────

def get_active_subscription(user_id: str) -> Optional[Dict[str, Any]]:
    db = get_mongo_db()
    if db is None:
        return None
    now = _now()
    return db.subscriptions.find_one(
        {
            "user_id": user_id,
            "status": "active",
            "is_active": True,
            "end_date": {"$gt": now},
        },
        sort=[("end_date", -1)]
    )


def get_active_subscription_by_email(email: str) -> Optional[Dict[str, Any]]:
    db = get_mongo_db()
    if db is None:
        return None
    user = get_user_by_email(email)
    if not user:
        return None
    return get_active_subscription(user["id"])


def create_or_extend_subscription(
    user_id: str,
    email: str,
    plan: str,
    duration_days: int,
) -> Dict[str, Any]:
    db = get_mongo_db()
    now = _now()
    active_sub = get_active_subscription(user_id)

    if active_sub and active_sub.get("end_date") and active_sub["end_date"] > now:
        # Extend from current end date
        new_end = active_sub["end_date"] + timedelta(days=duration_days)
        db.subscriptions.update_one(
            {"id": active_sub["id"]},
            {
                "$set": {
                    "plan": plan,
                    "end_date": new_end,
                    "updated_at": now,
                }
            }
        )
        active_sub["end_date"] = new_end
        active_sub["plan"] = plan
        return active_sub

    # Create fresh subscription
    new_end = now + timedelta(days=duration_days)
    sub_doc = {
        "id": str(uuid.uuid4()),
        "user_id": user_id,
        "email": email.lower().strip(),
        "plan": plan,
        "status": "active",
        "start_date": now,
        "end_date": new_end,
        "is_active": True,
        "created_at": now,
        "updated_at": now,
    }
    db.subscriptions.insert_one(sub_doc)
    return sub_doc


def cancel_subscription(user_id: str) -> bool:
    db = get_mongo_db()
    if db is None:
        return False
    res = db.subscriptions.update_many(
        {"user_id": user_id, "status": "active"},
        {"$set": {"status": "cancelled", "is_active": False, "updated_at": _now()}}
    )
    return res.modified_count > 0


# ── Payments ─────────────────────────────────────────────────────────────────

def create_payment(
    user_id: str,
    order_id: str,
    amount: int,
    currency: str,
    plan: str,
    status: str = "pending",
) -> Dict[str, Any]:
    db = get_mongo_db()
    doc = {
        "id": str(uuid.uuid4()),
        "user_id": user_id,
        "order_id": order_id,
        "payment_id": None,
        "amount": amount,
        "currency": currency,
        "plan": plan,
        "status": status,
        "created_at": _now(),
        "updated_at": _now(),
    }
    db.payments.insert_one(doc)
    return doc


def get_payment_by_order_id(order_id: str) -> Optional[Dict[str, Any]]:
    db = get_mongo_db()
    if db is None:
        return None
    return db.payments.find_one({"order_id": order_id})


def update_payment_status(
    order_id: str,
    payment_id: Optional[str],
    status: str,
) -> Optional[Dict[str, Any]]:
    db = get_mongo_db()
    if db is None:
        return None
    update_data: Dict[str, Any] = {"status": status, "updated_at": _now()}
    if payment_id:
        update_data["payment_id"] = payment_id
    res = db.payments.find_one_and_update(
        {"order_id": order_id},
        {"$set": update_data},
        return_document=True,
    )
    return res
