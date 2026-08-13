from contextlib import asynccontextmanager
from fastapi import FastAPI, UploadFile, File, BackgroundTasks
from fastapi.staticfiles import StaticFiles
from faster_whisper import WhisperModel
from fastapi.middleware.cors import CORSMiddleware

from pathlib import Path
import shutil
import uuid
import time

from llm_service import correct_english
from tts_service import text_to_speech


# ============================================================
# 1. DIRECTORIES & CLEANUP HELPER
# ============================================================

BASE_DIR = Path(__file__).parent
UPLOAD_DIR = BASE_DIR / "uploads"
AUDIO_DIR = BASE_DIR / "audio"

UPLOAD_DIR.mkdir(exist_ok=True)
AUDIO_DIR.mkdir(exist_ok=True)


def cleanup_old_files(max_age_seconds: int = 7200):
    """Deletes temporary uploads and audio files older than max_age_seconds (default 2 hours)."""
    now = time.time()
    for folder in [UPLOAD_DIR, AUDIO_DIR]:
        for item in folder.iterdir():
            if item.is_file() and not item.name.startswith(".git"):
                try:
                    if (now - item.stat().st_mtime) > max_age_seconds:
                        item.unlink()
                except Exception as e:
                    print(f"Error deleting temp file {item}: {e}")


# ============================================================
# 2. LIFESPAN EVENT HANDLER
# ============================================================

@asynccontextmanager
async def lifespan(app: FastAPI):
    cleanup_old_files(max_age_seconds=3600)
    print()
    print("========================================")
    print("English Tutor API is LIVE")
    print("========================================")
    print("API:     http://127.0.0.1:8000")
    print("Docs:    http://127.0.0.1:8000/docs")
    print("Audio:   http://127.0.0.1:8000/audio/")
    print("========================================")
    yield


# ============================================================
# 3. CREATE FASTAPI APP
# ============================================================

app = FastAPI(
    title="English Tutor API",
    description="AI English speaking tutor",
    version="1.0.0",
    lifespan=lifespan
)


app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ============================================================
# 4. SERVE GENERATED AUDIO FILES
# ============================================================

app.mount(
    "/audio",
    StaticFiles(directory=str(AUDIO_DIR)),
    name="audio"
)


# ============================================================
# 5. LOAD WHISPER
# ============================================================

print("========================================")
print("Loading Whisper model...")
print("========================================")

whisper_model = WhisperModel(
    "base.en",
    device="cpu",
    compute_type="int8"
)

print("Whisper model loaded.")
print()


# ============================================================
# 6. HOME ENDPOINT
# ============================================================

@app.get("/")
def home():
    return {
        "message": "English Tutor API is running",
        "status": "ok"
    }


# ============================================================
# 7. HEALTH CHECK
# ============================================================

@app.get("/health")
def health():
    return {
        "status": "healthy",
        "whisper": "loaded",
        "mistral": "configured",
        "tts": "configured"
    }


# ============================================================
# 8. SPEECH → TEXT
# ============================================================

@app.post("/transcribe")
def transcribe(file: UploadFile = File(...), background_tasks: BackgroundTasks = None):
    filename = f"{uuid.uuid4()}_{file.filename}"
    file_path = UPLOAD_DIR / filename

    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    segments, info = whisper_model.transcribe(
        str(file_path),
        beam_size=1,
        language="en",
        vad_filter=True
    )

    text = " ".join(segment.text.strip() for segment in segments)

    if background_tasks:
        background_tasks.add_task(cleanup_old_files)

    return {"text": text}


# ============================================================
# 9. COMPLETE AI TUTOR
# ============================================================

@app.post("/tutor")
def tutor(file: UploadFile = File(...), background_tasks: BackgroundTasks = None):
    # STEP 1: SAVE AUDIO
    filename = f"{uuid.uuid4()}_{file.filename}"
    file_path = UPLOAD_DIR / filename

    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    print()
    print("========================================")
    print("ENGLISH TUTOR")
    print("========================================")
    print("Audio:", file_path)

    # STEP 2: SPEECH → TEXT
    segments, info = whisper_model.transcribe(
        str(file_path),
        beam_size=1,
        language="en",
        vad_filter=True
    )

    text = " ".join(segment.text.strip() for segment in segments)
    print("Student said:", text)

    if not text.strip():
        text = "Hello, I am practicing my English."

    # STEP 3: TEXT → MISTRAL
    correction = correct_english(text)
    print("Mistral result:", correction)

    # STEP 4: GET CORRECTED SENTENCE
    corrected_text = correction.get("corrected", text)

    # STEP 5: TEXT → SPEECH
    audio_file = text_to_speech(corrected_text)
    audio_filename = Path(audio_file).name

    # STEP 6: SCHEDULE CLEANUP IN BACKGROUND
    if background_tasks:
        background_tasks.add_task(cleanup_old_files)

    return {
        "transcription": text,
        "correction": correction,
        "audio_url": f"/audio/{audio_filename}"
    }


# ============================================================
# 10. ENTRY POINT - START UVICORN SERVER AUTOMATICALLY
# ============================================================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)