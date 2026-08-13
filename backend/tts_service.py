import uuid
import wave
from pathlib import Path
from piper import PiperVoice

BASE_DIR = Path(__file__).parent
VOICE_MODEL = BASE_DIR / "en_US-lessac-medium.onnx"
AUDIO_DIR = BASE_DIR / "audio"
AUDIO_DIR.mkdir(exist_ok=True)

print("========================================")
print("Loading Piper TTS voice model...")
print("========================================")
try:
    piper_voice = PiperVoice.load(str(VOICE_MODEL))
    print("Piper TTS voice model loaded successfully.")
except Exception as e:
    print(f"Warning: Could not load PiperVoice directly: {e}")
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
        sys.executable,
        "-m",
        "piper",
        "-m",
        str(VOICE_MODEL),
        "-f",
        str(output_path),
        "--",
        text
    ]
    subprocess.run(command, check=True)
    return str(output_path)