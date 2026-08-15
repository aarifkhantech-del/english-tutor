from pydantic import BaseModel, Field
from typing import Optional


class CorrectionResult(BaseModel):
    hindi_input: str = Field(..., description="Original Hindi/Hinglish speech transcribed")
    english_translation: str = Field(..., description="Natural English translation")
    corrected: str = Field(..., description="Corrected English sentence")
    explanation: str = Field(default="", description="Short plain text grammar/vocabulary tip")
    practice: str = Field(default="", description="English practice sentence")
    encouragement: str = Field(default="", description="Motivational feedback note")


class TutorResponse(BaseModel):
    transcription: str = Field(..., description="Transcribed student Hindi text")
    correction: CorrectionResult = Field(..., description="AI tutor correction and explanation")
    audio_b64: str = Field(default="", description="Base64-encoded in-memory MP3 audio pronunciation")


class TranscribeResponse(BaseModel):
    text: str = Field(..., description="Transcribed text from speech")


class HealthResponse(BaseModel):
    status: str = "healthy"
    whisper: str = "loaded"
    mistral: str = "configured"
    tts: str = "configured"
    version: str = "2.1.0"
