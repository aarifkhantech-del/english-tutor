import logging
from fastapi import APIRouter, UploadFile, File, HTTPException
from app.models.schemas import TutorResponse
from app.services.asr_service import asr_service
from app.services.llm_service import llm_service
from app.services.tts_service import tts_service

logger = logging.getLogger(__name__)
router = APIRouter(tags=["AI Tutor"])


@router.post("/tutor", response_model=TutorResponse)
async def tutor_interaction(file: UploadFile = File(...)) -> TutorResponse:
    """Full end-to-end Hindi-to-English tutoring pipeline.
    
    1. Transcribes speech to Hindi/Hinglish text in memory.
    2. Corrects, translates, and generates explanations via Mistral AI.
    3. Synthesizes English pronunciation to in-memory Base64 MP3.
    """
    try:
        # STEP 1: READ AUDIO IN-MEMORY
        audio_bytes = await file.read()
        if not audio_bytes:
            raise HTTPException(status_code=400, detail="Audio file is empty.")

        # STEP 2: SPEECH → TEXT (WHISPER IN-MEMORY)
        transcription = await asr_service.transcribe(audio_bytes)
        logger.info("Transcribed text: '%s'", transcription)

        if not transcription:
            transcription = "नमस्ते, मैं अंग्रेजी सीखना चाहता हूँ"

        # STEP 3: TEXT → MISTRAL AI TUTOR
        correction = await llm_service.correct_and_translate(transcription)
        logger.info("Correction generated: '%s'", correction.english_translation)

        # STEP 4: TEXT → IN-MEMORY TTS AUDIO
        target_english = correction.english_translation or correction.corrected
        audio_b64 = await tts_service.generate_audio_b64(target_english)

        # STEP 5: RETURN TYPED PAYLOAD
        return TutorResponse(
            transcription=transcription,
            correction=correction,
            audio_b64=audio_b64,
        )
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Tutor pipeline error: %s", exc, exc_info=True)
        raise HTTPException(status_code=500, detail=f"AI Tutor pipeline failed: {exc}")
