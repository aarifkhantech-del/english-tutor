#!/usr/bin/env python
"""
Migration script to transfer data from the local SQLite database (english_tutor.db)
to the MongoDB Atlas cluster configured in the environment.
"""

from fastapi import testclient
import os
import sqlite3
import uuid
from datetime import datetime, timezone
from pathlib import Path
from dotenv import load_dotenv
from pymongo import MongoClient

# Load environment variables
BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env")

DB_PATH = BASE_DIR / "english_tutor.db"
MONGODB_URI = os.getenv("MONGODB_URI", "")
MONGODB_DB_NAME = os.getenv("MONGODB_DB_NAME", "vocalbharat")

print("======================================================================")
print("             VocalBharat SQLite -> MongoDB Migration                  ")
print("======================================================================")
print(f"SQLite DB: {DB_PATH}")
print(f"MongoDB Target DB: {MONGODB_DB_NAME}")
print("======================================================================\n")

if not DB_PATH.exists():
    print(f"Error: SQLite database file not found at {DB_PATH}")
    exit(1)

if not MONGODB_URI:
    print("Error: MONGODB_URI is not configured in your .env file.")
    exit(1)


def parse_dt(dt_str):
    if not dt_str:
        return None
    try:
        clean_str = dt_str.replace("Z", "+00:00")
        dt = datetime.fromisoformat(clean_str)
        # Ensure it has timezone info
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt
    except Exception:
        # Fallbacks
        for fmt in (
            "%Y-%m-%d %H:%M:%S.%f",
            "%Y-%m-%d %H:%M:%S",
            "%Y-%m-%dT%H:%M:%S.%f",
            "%Y-%m-%dT%H:%M:%S",
        ):
            try:
                dt = datetime.strptime(dt_str, fmt)
                return dt.replace(tzinfo=timezone.utc)
            except ValueError:
                continue
        return None


def main():
    # 1. Connect to SQLite
    print("Connecting to SQLite...")
    sqlite_conn = sqlite3.connect(DB_PATH)
    sqlite_conn.row_factory = sqlite3.Row
    cursor = sqlite_conn.cursor()

    # 2. Connect to MongoDB
    print("Connecting to MongoDB Atlas...")
    try:
        # Check if authSource is admin but fails, try fallback/options
        client = MongoClient(MONGODB_URI, serverSelectionTimeoutMS=5000)
        # Ping
        client.admin.command("ping")
        print("\nMongoDB Server Information:")
        print(client.admin.command("hello"))

        print("\nMongoDB Build Information:")
        print(client.admin.command("buildInfo"))
        db = client[MONGODB_DB_NAME]
        print("Connected to MongoDB successfully!\n")
    except Exception as e:
        print(f"Failed to connect to MongoDB: {e}")
        print("\nPlease verify the MONGODB_URI password and network/IP access permissions.")
        print("Ensure your current IP address is whitelisted in MongoDB Atlas Network Access.")
        exit(1)

    # 3. Read Users
    cursor.execute("SELECT * FROM users")
    sql_users = cursor.fetchall()
    print(f"Found {len(sql_users)} users in SQLite.")

    # Create mappings to resolve user emails for other collections
    user_map = {}  # user_id -> email
    for u in sql_users:
        user_map[u["id"]] = u["email"]

    # 4. Migrate Users
    users_migrated = 0
    for u in sql_users:
        created_at = parse_dt(u["created_at"]) or datetime.now(timezone.utc)
        last_login_at = parse_dt(u["last_login_at"])
        
        user_doc = {
            "id": u["id"],
            "email": u["email"].lower().strip(),
            "is_active": bool(u["is_active"]),
            "request_count": int(u["request_count"]),
            "created_at": created_at,
            "updated_at": created_at,
        }
        if last_login_at:
            user_doc["last_login_at"] = last_login_at

        # Upsert user by unique email
        db.users.update_one(
            {"email": user_doc["email"]},
            {"$set": user_doc},
            upsert=True
        )
        users_migrated += 1

    print(f"-> Successfully migrated/updated {users_migrated} users in MongoDB.")

    # 5. Migrate Subscriptions
    cursor.execute("SELECT * FROM subscriptions")
    sql_subs = cursor.fetchall()
    print(f"\nFound {len(sql_subs)} subscriptions in SQLite.")
    
    subs_migrated = 0
    sub_plan_map = {}  # sub_id -> plan (to associate with payments)
    for s in sql_subs:
        user_id = s["user_id"]
        email = user_map.get(user_id, "unknown@vocalbharat.com")
        start_date = parse_dt(s["starts_at"]) or parse_dt(s["created_at"]) or datetime.now(timezone.utc)
        end_date = parse_dt(s["expires_at"])
        created_at = parse_dt(s["created_at"]) or start_date
        updated_at = parse_dt(s["updated_at"]) or created_at
        
        sub_plan_map[s["id"]] = s["plan"]

        # Calculate if active
        is_active = False
        if s["status"] == "active" and end_date:
            is_active = end_date > datetime.now(timezone.utc)

        sub_doc = {
            "id": s["id"],
            "user_id": user_id,
            "email": email.lower().strip(),
            "plan": s["plan"],
            "status": s["status"],
            "start_date": start_date,
            "end_date": end_date,
            "is_active": is_active,
            "created_at": created_at,
            "updated_at": updated_at,
        }

        # Upsert subscription by unique ID
        db.subscriptions.update_one(
            {"id": sub_doc["id"]},
            {"$set": sub_doc},
            upsert=True
        )
        subs_migrated += 1

    print(f"-> Successfully migrated/updated {subs_migrated} subscriptions in MongoDB.")

    # 6. Migrate OTP Requests
    cursor.execute("SELECT * FROM otp_requests")
    sql_otps = cursor.fetchall()
    print(f"\nFound {len(sql_otps)} OTP requests in SQLite.")
    
    otps_migrated = 0
    for o in sql_otps:
        user_id = o["user_id"]
        email = user_map.get(user_id, "unknown@vocalbharat.com")
        created_at = parse_dt(o["created_at"]) or datetime.now(timezone.utc)
        expires_at = parse_dt(o["expires_at"]) or (created_at + datetime.timedelta(minutes=10))

        otp_doc = {
            "id": o["id"],
            "user_id": user_id,
            "email": email.lower().strip(),
            "otp_hash": o["code_hash"],  # Mapping SQLite code_hash -> otp_hash
            "attempts": int(o["attempts"]),
            "is_used": bool(o["is_used"]),
            "expires_at": expires_at,
            "created_at": created_at,
        }

        # Upsert by ID
        db.otp_requests.update_one(
            {"id": otp_doc["id"]},
            {"$set": otp_doc},
            upsert=True
        )
        otps_migrated += 1

    print(f"-> Successfully migrated/updated {otps_migrated} OTP requests in MongoDB.")

    # 7. Migrate Payments
    cursor.execute("SELECT * FROM payments")
    sql_payments = cursor.fetchall()
    print(f"\nFound {len(sql_payments)} payments in SQLite.")
    
    payments_migrated = 0
    for p in sql_payments:
        user_id = p["user_id"]
        sub_id = p["subscription_id"]
        plan = sub_plan_map.get(sub_id, "monthly") if sub_id else "monthly"
        created_at = parse_dt(p["created_at"]) or datetime.now(timezone.utc)
        updated_at = parse_dt(p["updated_at"]) or created_at

        pay_doc = {
            "id": p["id"],
            "user_id": user_id,
            "order_id": p["gateway_order_id"],
            "payment_id": p["gateway_payment_id"],
            "amount": int(p["amount"]),
            "currency": p["currency"],
            "plan": plan,
            "status": p["status"],
            "created_at": created_at,
            "updated_at": updated_at,
        }

        # Upsert by ID
        db.payments.update_one(
            {"id": pay_doc["id"]},
            {"$set": pay_doc},
            upsert=True
        )
        payments_migrated += 1

    print(f"-> Successfully migrated/updated {payments_migrated} payments in MongoDB.")

    # Close SQLite
    sqlite_conn.close()
    print("\n======================================================================")
    print("                     Migration Completed successfully!                ")
    print("======================================================================")


if __name__ == "__main__":
    main()
