import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from fastapi.testclient import TestClient
from app.main import app
from app.core.database import Base, engine, SessionLocal
from app.models.db_models import User, OTPRequest, Subscription, Payment

client = TestClient(app)

import uuid

def test_complete_auth_and_subscription_flow():
    # Setup test DB tables
    Base.metadata.create_all(bind=engine)
    
    test_email = f"student_{uuid.uuid4().hex[:8]}@example.com"

    # 1. Request OTP
    res = client.post("/auth/request-otp", json={"email": test_email})
    assert res.status_code == 200, res.text
    data = res.json()
    assert "OTP sent" in data["message"]
    assert data["email"] == test_email

    # Retrieve OTP code hash from DB and find user
    db = SessionLocal()
    user = db.query(User).filter(User.email == test_email).first()
    assert user is not None
    otp_record = db.query(OTPRequest).filter(OTPRequest.user_id == user.id, OTPRequest.is_used == False).first()
    assert otp_record is not None

    # Test with valid OTP (we compute hash of plain text)
    # We can inject a known hash for testing
    plain_test_otp = "998877"
    otp_record.code_hash = OTPRequest.hash_code(plain_test_otp)
    db.commit()
    db.close()

    # 2. Verify OTP
    res = client.post("/auth/verify-otp", json={"email": test_email, "otp": plain_test_otp})
    assert res.status_code == 200, res.text
    token_data = res.json()
    assert "access_token" in token_data
    token = token_data["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 3. Check /auth/me
    res = client.get("/auth/me", headers=headers)
    assert res.status_code == 200
    me_data = res.json()
    assert me_data["email"] == test_email

    # 4. Check /subscription/plans
    res = client.get("/subscription/plans")
    assert res.status_code == 200
    plans = res.json()["plans"]
    assert any(p["id"] == "monthly" and p["amount"] == 300 and p["duration_days"] == 30 for p in plans)

    # 5. Check initial subscription status (should be false/free tier)
    res = client.get("/subscription/status", headers=headers)
    assert res.status_code == 200
    status_data = res.json()
    assert status_data["is_active"] is False
    assert status_data["requests_limit"] == 20

    # 6. Initiate Monthly Plan (₹300)
    res = client.post("/subscription/initiate", json={"plan": "monthly"}, headers=headers)
    assert res.status_code == 200, res.text
    init_data = res.json()
    order_id = init_data["order_id"]
    assert init_data["amount"] == 300

    # 7. Confirm payment
    res = client.post("/subscription/confirm", json={
        "order_id": order_id,
        "payment_id": "mock_pay_12345",
        "signature": None
    }, headers=headers)
    assert res.status_code == 200, res.text
    sub_data = res.json()
    assert sub_data["is_active"] is True
    assert sub_data["plan"] == "monthly"
    assert sub_data["days_remaining"] == 30

    # 8. Check subscription status again
    res = client.get("/subscription/status", headers=headers)
    assert res.status_code == 200
    final_status = res.json()
    assert final_status["is_active"] is True
    assert final_status["plan"] == "monthly"
    print("\n[SUCCESS] All Auth & Subscription tests passed successfully!")

if __name__ == "__main__":
    test_complete_auth_and_subscription_flow()
