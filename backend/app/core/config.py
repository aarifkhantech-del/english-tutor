import os
from pathlib import Path
from dotenv import load_dotenv

# Base backend directory
BASE_DIR = Path(__file__).resolve().parent.parent.parent

# Explicitly load .env from backend root
load_dotenv(BASE_DIR / ".env")


class Settings:
    PROJECT_NAME: str = "Jio English - Spoken English Tutor"
    VERSION: str = "2.1.0"
    DESCRIPTION: str = "High-performance AI Spoken English tutor for Hindi speakers"

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


settings = Settings()
