import asyncio
import io
import logging
from faster_whisper import WhisperModel
from app.core.config import settings

logger = logging.getLogger(__name__)


class ASRService:
    """Automated Speech Recognition Service using Faster-Whisper."""

    _instance: "ASRService | None" = None
    _model: WhisperModel | None = None

    def __new__(cls) -> "ASRService":
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def initialize(self) -> None:
        """Loads the Whisper model into memory if not already loaded."""
        if self._model is None:
            logger.info("Loading Multilingual Whisper model (%s on %s)...", 
                        settings.WHISPER_MODEL_SIZE, settings.WHISPER_DEVICE)
            self._model = WhisperModel(
                settings.WHISPER_MODEL_SIZE,
                device=settings.WHISPER_DEVICE,
                compute_type=settings.WHISPER_COMPUTE_TYPE,
            )
            logger.info("Whisper model successfully loaded.")

    @property
    def is_loaded(self) -> bool:
        return self._model is not None

    def _sync_transcribe(self, audio_bytes: bytes) -> str:
        """Synchronous CPU-bound transcription."""
        if self._model is None:
            self.initialize()

        assert self._model is not None
        segments, _ = self._model.transcribe(
            io.BytesIO(audio_bytes),
            beam_size=1,
            vad_filter=True,
            vad_parameters=dict(min_silence_duration_ms=500),
            temperature=0.0,
            condition_on_previous_text=False,
        )

        return " ".join(segment.text.strip() for segment in segments).strip()

    async def transcribe(self, audio_bytes: bytes) -> str:
        """Non-blocking async transcription offloaded to thread pool."""
        if not audio_bytes:
            return ""
        return await asyncio.to_thread(self._sync_transcribe, audio_bytes)


# Global singleton instance
asr_service = ASRService()
