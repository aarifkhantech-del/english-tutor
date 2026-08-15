import uuid
from pathlib import Path
from gtts import gTTS

BASE_DIR = Path(__file__).parent
AUDIO_DIR = BASE_DIR / "audio"
AUDIO_DIR.mkdir(exist_ok=True)

print("========================================")
print("gTTS (Google Text-to-Speech) ready.")
print("========================================")


def text_to_speech(text: str) -> str:
    """Convert text to speech using gTTS and save as an MP3 file.

    Args:
        text: The text to synthesize.

    Returns:
        The absolute path to the generated audio file.
    """
    filename = f"{uuid.uuid4()}.mp3"
    output_path = AUDIO_DIR / filename

    tts = gTTS(text=text, lang="en", slow=False)
    tts.save(str(output_path))

    return str(output_path)