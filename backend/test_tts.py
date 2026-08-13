from tts_service import text_to_speech


text = "I went to the market yesterday."

audio_file = text_to_speech(text)

print("Audio generated:")
print(audio_file)