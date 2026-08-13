from faster_whisper import WhisperModel

print("Loading Whisper model...")

model = WhisperModel(
    "base.en",
    device="cpu",
    compute_type="int8"
)

print("Model loaded.")

segments, info = model.transcribe(
    "Recording.m4a"
)

text = " ".join(segment.text for segment in segments)

print()
print("Recognized text:")
print(text)