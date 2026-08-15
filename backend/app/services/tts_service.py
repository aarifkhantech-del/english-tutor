import asyncio
import base64
import io
import logging
from gtts import gTTS
from app.core.config import settings

logger = logging.getLogger(__name__)


class TTSService:
    """Text-to-Speech Service using in-memory gTTS streaming."""

    def _sync_generate_bytes(self, text: str) -> bytes:
        """Synchronously synthesizes text to MP3 bytes."""
        buffer = io.BytesIO()
        tts = gTTS(text=text, lang=settings.TTS_LANG, slow=settings.TTS_SLOW)
        tts.write_to_fp(buffer)
        return buffer.getvalue()

    async def generate_audio_bytes(self, text: str) -> bytes:
        """Non-blocking async generation of MP3 audio bytes."""
        if not text.strip():
            return b""
        return await asyncio.to_thread(self._sync_generate_bytes, text)

    async def generate_audio_b64(self, text: str) -> str:
        """Non-blocking async generation of Base64-encoded MP3 audio."""
        audio_bytes = await self.generate_audio_bytes(text)
        if not audio_bytes:
            return ""
        return base64.b64encode(audio_bytes).decode("utf-8")


# Global service instance
tts_service = TTSService()
