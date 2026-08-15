import requests
import json
import os
from pathlib import Path
from dotenv import load_dotenv

# Load environment variables from .env file
env_path = Path(__file__).parent / ".env"
load_dotenv(dotenv_path=env_path)

MISTRAL_API_KEY = os.getenv("MISTRAL_API_KEY", "")
MISTRAL_URL = "https://api.mistral.ai/v1/chat/completions"
MODEL_ID = "mistral-small-latest"

# Persistent HTTP session for TCP/SSL connection pooling
session = requests.Session()
if MISTRAL_API_KEY:
    session.headers.update({
        "Authorization": f"Bearer {MISTRAL_API_KEY}",
        "Content-Type": "application/json"
    })

SYSTEM_PROMPT = """
You are an expert Hindi-to-English Tutor.

Your job is to help Hindi speakers learn spoken English.

When the student gives you a sentence in Hindi or Hinglish:
1. Provide the natural, grammatically correct English translation ("english_translation" and "corrected").
2. Show the Hindi text as received ("hindi_input").
3. Write a short explanation of the key vocabulary and grammar as a SINGLE PLAIN TEXT STRING in simple English ("explanation"). Do NOT use nested objects or arrays - just one plain string.
4. Provide one short English practice sentence ("practice").
5. Give a short encouraging note ("encouragement").

CRITICAL RULES:
- ALL values must be plain strings. No nested JSON objects or arrays allowed inside values.
- "explanation" must be a single readable string like: "'जा रहा हूँ' means 'going'. This uses Present Continuous tense: subject + am/is/are + verb+ing."
- Return ONLY valid JSON with exactly these 6 string fields:

{
    "hindi_input": "original Hindi sentence here",
    "english_translation": "natural English translation here",
    "corrected": "same as english_translation",
    "explanation": "short plain-text grammar and vocabulary tip here",
    "practice": "one short English practice sentence here",
    "encouragement": "short motivating note here"
}
"""


def correct_english(sentence: str) -> dict:
    if not MISTRAL_API_KEY:
        print("Warning: MISTRAL_API_KEY is not set in environment or .env file.")

    payload = {
        "model": MODEL_ID,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": sentence}
        ],
        "temperature": 0.2,
        "max_tokens": 300,
        "response_format": {"type": "json_object"}
    }

    try:
        headers = {
            "Authorization": f"Bearer {os.getenv('MISTRAL_API_KEY', MISTRAL_API_KEY)}",
            "Content-Type": "application/json"
        }
        response = session.post(
            MISTRAL_URL,
            headers=headers,
            json=payload,
            timeout=15
        )
        response.raise_for_status()
        result = response.json()
        answer = result["choices"][0]["message"]["content"]

        print("Mistral response:")
        print(answer)

        parsed = json.loads(answer)
        if "corrected" not in parsed:
            parsed["corrected"] = parsed.get("english_translation", sentence)
        return parsed
    except Exception as e:
        print(f"Mistral API error or fallback: {e}")
        return {
            "hindi_input": sentence,
            "english_translation": sentence,
            "corrected": sentence,
            "explanation": "Translation completed.",
            "practice": "Try repeating the sentence out loud in English.",
            "encouragement": "Keep practicing your spoken English!"
        }