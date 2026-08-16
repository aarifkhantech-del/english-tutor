import hashlib
import hmac
import sys
from pathlib import Path

# Add backend root to sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from fastapi.testclient import TestClient
from app.main import app
from app.core.config import settings

client = TestClient(app)


def test_payment_config_endpoint():
    """Verify that public payment config returns the configured Razorpay Key ID."""
    response = client.get("/api/payment-config")
    assert response.status_code == 200
    data = response.json()
    assert "key_id" in data
    assert data["key_id"] == settings.RAZORPAY_KEY_ID
    assert data["currency"] == "INR"


def test_create_order_validation_minimum_amount():
    """Verify that orders below 100 paise (₹1.00) are rejected with 400 Bad Request."""
    response = client.post("/api/create-order", json={"amount": 50, "currency": "INR"})
    assert response.status_code == 400
    assert "at least 100 paise" in response.json()["detail"]


def test_create_order_success():
    """Verify that creating an order with valid amount calls Razorpay and returns order details."""
    response = client.post("/api/create-order", json={
        "amount": 30000,
        "currency": "INR",
        "receipt": "rcpt_test_12345",
        "notes": {"plan": "monthly_pro"}
    })
    # If network/API key allows real order or mock, assert proper structure
    assert response.status_code == 200, response.text
    data = response.json()
    assert "order_id" in data
    assert data["order_id"].startswith("order_")
    assert data["amount"] == 30000
    assert data["currency"] == "INR"
    assert data["key_id"] == settings.RAZORPAY_KEY_ID


def test_verify_payment_missing_fields():
    """Verify that missing order_id, payment_id, or signature returns 400 Bad Request."""
    response = client.post("/api/verify-payment", json={
        "order_id": "order_dummy_123",
        # missing payment_id and signature
    })
    assert response.status_code == 400
    assert "Missing required fields" in response.json()["detail"]


def test_verify_payment_invalid_signature():
    """Verify that an incorrect signature returns 400 Bad Request."""
    response = client.post("/api/verify-payment", json={
        "razorpay_order_id": "order_test_12345",
        "razorpay_payment_id": "pay_test_67890",
        "razorpay_signature": "invalid_signature_hash_hex"
    })
    assert response.status_code == 400
    assert "Signature mismatch" in response.json()["detail"]


def test_verify_payment_valid_signature():
    """Verify that a valid HMAC-SHA256 signature succeeds with 200 OK."""
    order_id = "order_test_ABC123"
    payment_id = "pay_test_XYZ789"
    
    # Generate valid signature matching secret key
    message = f"{order_id}|{payment_id}"
    secret_bytes = settings.RAZORPAY_KEY_SECRET.encode("utf-8")
    valid_signature = hmac.new(secret_bytes, message.encode("utf-8"), hashlib.sha256).hexdigest()

    response = client.post("/api/verify-payment", json={
        "razorpay_order_id": order_id,
        "razorpay_payment_id": payment_id,
        "razorpay_signature": valid_signature
    })
    assert response.status_code == 200, response.text
    data = response.json()
    assert data["status"] == "success"
    assert data["order_id"] == order_id
    assert data["payment_id"] == payment_id


def test_checkout_web_page_serving():
    """Verify that static checkout HTML page is served correctly at /, /checkout, and /app."""
    # Test browser GET with text/html accept header
    res_browser = client.get("/", headers={"Accept": "text/html"})
    assert res_browser.status_code == 200
    assert "VocalBharat" in res_browser.text
    assert "checkout.razorpay.com/v1/checkout.js" in res_browser.text

    for path in ["/checkout", "/app"]:
        response = client.get(path)
        assert response.status_code == 200
        assert "VocalBharat" in response.text
        assert "checkout.razorpay.com/v1/checkout.js" in response.text



if __name__ == "__main__":
    test_payment_config_endpoint()
    test_create_order_validation_minimum_amount()
    test_create_order_success()
    test_verify_payment_missing_fields()
    test_verify_payment_invalid_signature()
    test_verify_payment_valid_signature()
    test_checkout_web_page_serving()
    print("\n[SUCCESS] All Razorpay integration tests passed!")
