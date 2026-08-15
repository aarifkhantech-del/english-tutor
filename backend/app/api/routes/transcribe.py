from fastapi import APIRouter, UploadFile, File, HTTPException
from app.models.schemas import TranscribeResponse
from app.services.asr_service import asr_service

router = APIRouter(tags=["Speech Recognition"])


@router.post("/transcribe", response_model=TranscribeResponse)
async def transcribe_audio(file: UploadFile = File(...)) -> TranscribeResponse:
    """Transcribes an uploaded audio file directly from memory into text."""
    try:
        audio_bytes = await file.read()
        if not audio_bytes:
            raise HTTPException(status_code=400, detail="Empty audio file provided.")

        text = await asr_service.transcribe(audio_bytes)
        return TranscribeResponse(text=text)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Transcription failed: {exc}")
