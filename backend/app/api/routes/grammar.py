import logging
from fastapi import APIRouter, HTTPException, Depends
from app.models.schemas import GrammarRequest, GrammarResponse
from app.services.grammar_service import grammar_service
from app.core.security import verify_usage_quota

logger = logging.getLogger(__name__)
router = APIRouter(tags=["Grammar Explainer"])


@router.post("/explain", response_model=GrammarResponse)
async def explain_grammar(
    payload: GrammarRequest,
    _quota = Depends(verify_usage_quota),
) -> GrammarResponse:
    """Explain any English grammar topic with definition, Hindi translation, examples and tips."""
    topic = payload.topic.strip()
    if not topic:
        raise HTTPException(status_code=400, detail="Topic cannot be empty.")

    logger.info("Grammar explain request: '%s'", topic)
    try:
        result = await grammar_service.explain_topic(topic)
        return result
    except Exception as exc:
        logger.error("Grammar explainer error: %s", exc, exc_info=True)
        raise HTTPException(status_code=500, detail=f"Grammar explainer failed: {exc}")
