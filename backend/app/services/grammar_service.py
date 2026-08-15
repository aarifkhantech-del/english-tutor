import json
import logging
import httpx
from app.core.config import settings
from app.core.prompts import GRAMMAR_SYSTEM_PROMPT
from app.models.schemas import GrammarResponse, GrammarExample

logger = logging.getLogger(__name__)


class GrammarService:
    """Mistral AI service that explains English grammar topics with Hindi translations."""

    def __init__(self) -> None:
        self._client: httpx.AsyncClient | None = None

    def _get_client(self) -> httpx.AsyncClient:
        if self._client is None or self._client.is_closed:
            limits = httpx.Limits(max_keepalive_connections=20, max_connections=50)
            timeout = httpx.Timeout(settings.MISTRAL_TIMEOUT, connect=5.0)
            self._client = httpx.AsyncClient(limits=limits, timeout=timeout)
        return self._client

    async def close(self) -> None:
        if self._client and not self._client.is_closed:
            await self._client.aclose()
            self._client = None

    async def explain_topic(self, topic: str) -> GrammarResponse:
        """Call Mistral AI to explain an English grammar topic."""
        topic = topic.strip()
        if not topic:
            topic = "Present Tense"

        if not settings.MISTRAL_API_KEY:
            logger.warning("MISTRAL_API_KEY not configured. Using fallback grammar response.")
            return self._fallback_response(topic)

        payload = {
            "model": settings.MISTRAL_MODEL,
            "messages": [
                {"role": "system", "content": GRAMMAR_SYSTEM_PROMPT},
                {"role": "user", "content": f"Explain: {topic}"},
            ],
            "temperature": 0.3,
            "max_tokens": 700,
            "response_format": {"type": "json_object"},
        }

        headers = {
            "Authorization": f"Bearer {settings.MISTRAL_API_KEY}",
            "Content-Type": "application/json",
        }

        client = self._get_client()
        try:
            response = await client.post(settings.MISTRAL_URL, headers=headers, json=payload)
            response.raise_for_status()

            data = response.json()
            raw = data["choices"][0]["message"]["content"]
            parsed = json.loads(raw)

            # Build examples list safely
            raw_examples = parsed.get("examples", [])
            examples = []
            for ex in raw_examples:
                if isinstance(ex, dict):
                    examples.append(GrammarExample(
                        sentence=ex.get("sentence", ""),
                        explanation=ex.get("explanation", ""),
                    ))

            # Build tips list safely
            raw_tips = parsed.get("tips", [])
            tips = [str(t) for t in raw_tips if t]

            return GrammarResponse(
                topic=parsed.get("topic", topic),
                definition=parsed.get("definition", ""),
                hindi_definition=parsed.get("hindi_definition", ""),
                examples=examples,
                tips=tips,
                difficulty=parsed.get("difficulty", "Beginner"),
            )

        except Exception as exc:
            logger.error("Grammar API error: %s", exc, exc_info=True)
            return self._fallback_response(topic)

    def _fallback_response(self, topic: str) -> GrammarResponse:
        return GrammarResponse(
            topic=topic,
            definition=f"Unable to fetch explanation for '{topic}'. Please check your API key.",
            hindi_definition=f"'{topic}' की व्याख्या नहीं मिली। कृपया API key जाँचें।",
            examples=[
                GrammarExample(sentence="Service unavailable.", explanation="Backend error."),
            ],
            tips=["Check your internet connection.", "Verify API key in settings."],
            difficulty="Beginner",
        )


# Global service instance
grammar_service = GrammarService()
