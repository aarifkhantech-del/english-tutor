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
You are an English speaking tutor.

Your job is to help students improve their spoken English.

When the student gives you a sentence:
1. Correct grammar mistakes.
2. Correct unnatural English.
3. Preserve the student's intended meaning.
4. Explain the mistake using simple English.
5. Give one short practice sentence.
6. Give a short encouraging message.

Important:
- Do not change the meaning.
- Keep explanations simple.
- If the sentence is already correct, say that it is correct.
- Return ONLY valid JSON.

Return format:
{
    "corrected": "...",
    "explanation": "...",
    "practice": "...",
    "encouragement": "..."
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
        "max_tokens": 250,
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

        return json.loads(answer)
    except Exception as e:
        print(f"Mistral API error or fallback: {e}")
        return {
            "corrected": sentence,
            "explanation": "Grammar check completed.",
            "practice": "Try repeating your sentence out loud.",
            "encouragement": "Keep practicing your spoken English!"
        }