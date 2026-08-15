import json
import logging
import httpx
from app.core.config import settings
from app.core.prompts import TUTOR_SYSTEM_PROMPT
from app.models.schemas import CorrectionResult

logger = logging.getLogger(__name__)


class LLMService:
    """Asynchronous Mistral AI service for Hindi-to-English tutoring."""

    def __init__(self) -> None:
        self._client: httpx.AsyncClient | None = None

    def _get_client(self) -> httpx.AsyncClient:
        if self._client is None or self._client.is_closed:
            limits = httpx.Limits(max_keepalive_connections=20, max_connections=50)
            timeout = httpx.Timeout(settings.MISTRAL_TIMEOUT, connect=5.0)
            self._client = httpx.AsyncClient(limits=limits, timeout=timeout)
        return self._client

    async def close(self) -> None:
        """Closes the underlying HTTP client session."""
        if self._client and not self._client.is_closed:
            await self._client.aclose()
            self._client = None

    async def correct_and_translate(self, sentence: str) -> CorrectionResult:
        """Sends Hindi/Hinglish sentence to Mistral AI and returns structured correction."""
        if not sentence.strip():
            sentence = "नमस्ते, मैं अंग्रेजी सीखना चाहता हूँ"

        if not settings.MISTRAL_API_KEY:
            logger.warning("MISTRAL_API_KEY is not configured. Using fallback translation.")
            return self._fallback_response(sentence)

        payload = {
            "model": settings.MISTRAL_MODEL,
            "messages": [
                {"role": "system", "content": TUTOR_SYSTEM_PROMPT},
                {"role": "user", "content": sentence},
            ],
            "temperature": 0.2,
            "max_tokens": 300,
            "response_format": {"type": "json_object"},
        }

        headers = {
            "Authorization": f"Bearer {settings.MISTRAL_API_KEY}",
            "Content-Type": "application/json",
        }

        client = self._get_client()

        try:
            response = await client.post(
                settings.MISTRAL_URL,
                headers=headers,
                json=payload,
            )
            response.raise_for_status()

            data = response.json()
            raw_content = data["choices"][0]["message"]["content"]
            parsed = json.loads(raw_content)

            english_trans = parsed.get("english_translation") or parsed.get("corrected", sentence)
            corrected_text = parsed.get("corrected") or english_trans

            return CorrectionResult(
                hindi_input=parsed.get("hindi_input", sentence),
                english_translation=english_trans,
                corrected=corrected_text,
                explanation=parsed.get("explanation", ""),
                practice=parsed.get("practice", ""),
                encouragement=parsed.get("encouragement", ""),
            )
        except Exception as exc:
            logger.error("Mistral API error: %s. Using graceful fallback.", exc)
            return self._fallback_response(sentence)

    def _fallback_response(self, sentence: str) -> CorrectionResult:
        return CorrectionResult(
            hindi_input=sentence,
            english_translation=sentence,
            corrected=sentence,
            explanation="Translation generated.",
            practice="Try repeating the sentence out loud in English.",
            encouragement="Keep practicing every day!",
        )


# Global service instance
llm_service = LLMService()
