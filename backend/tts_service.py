import uuid
import wave
import urllib.request
from pathlib import Path
from piper import PiperVoice

BASE_DIR = Path(__file__).parent
VOICE_MODEL = BASE_DIR / "en_US-lessac-medium.onnx"
VOICE_CONFIG = BASE_DIR / "en_US-lessac-medium.onnx.json"
AUDIO_DIR = BASE_DIR / "audio"
AUDIO_DIR.mkdir(exist_ok=True)

# Hugging Face direct download URLs for the Piper voice model
HF_BASE = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium"
MODEL_URL = f"{HF_BASE}/en_US-lessac-medium.onnx"
CONFIG_URL = f"{HF_BASE}/en_US-lessac-medium.onnx.json"


def _download_voice_model():
    """Download the Piper voice model from Hugging Face if not present."""
    if not VOICE_MODEL.exists():
        print("Downloading Piper TTS voice model (~65 MB)...")
        try:
            urllib.request.urlretrieve(MODEL_URL, str(VOICE_MODEL))
            print("Voice model downloaded.")
        except Exception as e:
            print(f"ERROR: Failed to download voice model: {e}")
            raise

    if not VOICE_CONFIG.exists():
        print("Downloading Piper TTS voice config...")
        try:
            urllib.request.urlretrieve(CONFIG_URL, str(VOICE_CONFIG))
            print("Voice config downloaded.")
        except Exception as e:
            print(f"Warning: Could not download voice config: {e}")


print("========================================")
print("Loading Piper TTS voice model...")
print("========================================")

_download_voice_model()

try:
    piper_voice = PiperVoice.load(str(VOICE_MODEL))
    print("Piper TTS voice model loaded successfully.")
except Exception as e:
    print(f"Warning: Could not load PiperVoice: {e}")
    piper_voice = None


def text_to_speech(text: str) -> str:
    filename = f"{uuid.uuid4()}.wav"
    output_path = AUDIO_DIR / filename

    if piper_voice is not None:
        chunks = list(piper_voice.synthesize(text))
        if chunks:
            wav_file = wave.open(str(output_path), "wb")
            wav_file.setnchannels(chunks[0].sample_channels)
            wav_file.setsampwidth(chunks[0].sample_width)
            wav_file.setframerate(chunks[0].sample_rate)
            for chunk in chunks:
                wav_file.writeframes(chunk.audio_int16_bytes)
            wav_file.close()
            return str(output_path)

    # Fallback to CLI subprocess
    import subprocess
    import sys
    command = [
        sys.executable, "-m", "piper",
        "-m", str(VOICE_MODEL),
        "-f", str(output_path),
        "--", text
    ]
    subprocess.run(command, check=True)
    return str(output_path)