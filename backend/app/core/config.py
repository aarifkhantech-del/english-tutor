import os
from pathlib import Path
from dotenv import load_dotenv

# Base backend directory
BASE_DIR = Path(__file__).resolve().parent.parent.parent

# Explicitly load .env from backend root
load_dotenv(BASE_DIR / ".env")


class Settings:
    PROJECT_NAME: str = "VocalBharat - Spoken English Tutor"
    VERSION: str = "2.1.0"
    DESCRIPTION: str = "AI spoken English tutor for Hindi speakers: Hindi-to-English practice, pronunciation, and grammar."

    # Mistral AI configuration
    MISTRAL_API_KEY: str = os.getenv("MISTRAL_API_KEY", "")
    MISTRAL_URL: str = "https://api.mistral.ai/v1/chat/completions"
    MISTRAL_MODEL: str = os.getenv("MISTRAL_MODEL", "mistral-small-latest")
    MISTRAL_TIMEOUT: float = float(os.getenv("MISTRAL_TIMEOUT", "15.0"))

    # Whisper ASR configuration (tiny = sub-second ultra-fast transcription)
    WHISPER_MODEL_SIZE: str = os.getenv("WHISPER_MODEL_SIZE", "tiny")
    WHISPER_DEVICE: str = os.getenv("WHISPER_DEVICE", "cpu")
    WHISPER_COMPUTE_TYPE: str = os.getenv("WHISPER_COMPUTE_TYPE", "int8")

    # TTS configuration
    TTS_LANG: str = "en"
    TTS_SLOW: bool = False

    # Server configuration
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "8000"))
    CORS_ORIGINS: list[str] = ["*"]

    # ── Database (MongoDB only) ──────────────────────────────────────────────
    MONGODB_URI: str = os.getenv("MONGODB_URI", "")
    MONGODB_DB_NAME: str = os.getenv("MONGODB_DB_NAME", "vocalbharat")

    @property
    def use_mongodb(self) -> bool:
        return bool(self.MONGODB_URI.strip())

    @property
    def is_dev(self) -> bool:
        return os.getenv("ENVIRONMENT", "development") == "development"

    # ── JWT / Auth ────────────────────────────────────────────────────────────
    SECRET_KEY: str = os.getenv("SECRET_KEY", "change-me-please-use-a-long-random-string")
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRY_DAYS: int = int(os.getenv("JWT_EXPIRY_DAYS", "7"))

    # Google Sign-In. Use the Web OAuth client ID here; Android requests its ID
    # token for this audience and the API verifies it before issuing our JWT.
    GOOGLE_OAUTH_CLIENT_IDS: list[str] = [
        client_id.strip()
        for client_id in os.getenv("GOOGLE_OAUTH_CLIENT_IDS", "").split(",")
        if client_id.strip()
    ]

    @property
    def google_sign_in_enabled(self) -> bool:
        return bool(self.GOOGLE_OAUTH_CLIENT_IDS)

    # ── Email / SMTP ──────────────────────────────────────────────────────────
    # Leave SMTP_HOST empty → OTPs are printed to the terminal (dev mode)
    SMTP_HOST: str = os.getenv("SMTP_HOST", "")
    SMTP_PORT: int = int(os.getenv("SMTP_PORT", "587"))
    SMTP_USER: str = os.getenv("SMTP_USER", "")
    SMTP_PASSWORD: str = os.getenv("SMTP_PASSWORD", "")
    EMAIL_FROM: str = os.getenv("EMAIL_FROM", "VocalBharat <no-reply@vocalbharat.com>")

    @property
    def use_console_email(self) -> bool:
        return not self.SMTP_HOST

    # ── OTP ───────────────────────────────────────────────────────────────────
    OTP_EXPIRY_MINUTES: int = int(os.getenv("OTP_EXPIRY_MINUTES", "10"))
    OTP_MAX_ATTEMPTS: int = int(os.getenv("OTP_MAX_ATTEMPTS", "5"))
    OTP_RATE_LIMIT_COUNT: int = int(os.getenv("OTP_RATE_LIMIT_COUNT", "3"))
    OTP_RATE_LIMIT_WINDOW_MINUTES: int = int(os.getenv("OTP_RATE_LIMIT_WINDOW_MINUTES", "60"))

    # ── Usage Quota ───────────────────────────────────────────────────────────
    FREE_REQUESTS_LIMIT: int = int(os.getenv("FREE_REQUESTS_LIMIT", "8"))

    # ── Payment ───────────────────────────────────────────────────────────────
    # Set to 'razorpay', 'cashfree', or 'stripe' when ready
    PAYMENT_GATEWAY: str = os.getenv("PAYMENT_GATEWAY", "mock")
    PAYMENT_MONTHLY_AMOUNT: int = int(os.getenv("PAYMENT_MONTHLY_AMOUNT", "120"))
    PAYMENT_CURRENCY: str = os.getenv("PAYMENT_CURRENCY", "INR")

    # Razorpay
    RAZORPAY_KEY_ID: str = os.getenv("RAZORPAY_KEY_ID", "")
    RAZORPAY_KEY_SECRET: str = os.getenv("RAZORPAY_KEY_SECRET", "")

    # Stripe
    STRIPE_SECRET_KEY: str = os.getenv("STRIPE_SECRET_KEY", "")
    STRIPE_WEBHOOK_SECRET: str = os.getenv("STRIPE_WEBHOOK_SECRET", "")

    # Mock gateway
    MOCK_WEBHOOK_SECRET: str = os.getenv("MOCK_WEBHOOK_SECRET", "mock-secret-key")


settings = Settings()
