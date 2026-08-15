import asyncio
import sys
from pathlib import Path

# Ensure backend root on sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from httpx import AsyncClient, ASGITransport
from app.main import app
from app.services.tts_service import tts_service


async def run_all():
    print("========================================")
    print("Running Backend Verification Tests...")
    print("========================================")

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        # Test 1: Root endpoint
        res = await client.get("/")
        assert res.status_code == 200, f"Root failed: {res.status_code}"
        assert res.json()["status"] == "online"
        print("[PASS] Root endpoint: OK")

        # Test 2: Health endpoint
        res = await client.get("/health")
        assert res.status_code == 200, f"Health failed: {res.status_code}"
        assert res.json()["status"] == "healthy"
        print("[PASS] Health endpoint: OK")

        # Test 3: In-memory TTS generation
        audio_b64 = await tts_service.generate_audio_b64("Hello, how are you?")
        assert isinstance(audio_b64, str) and len(audio_b64) > 100
        print(f"[PASS] In-memory TTS synthesis: OK (b64 length: {len(audio_b64)})")

    print("========================================")
    print("ALL TESTS PASSED SUCCESSFULLY!")
    print("========================================")


if __name__ == "__main__":
    asyncio.run(run_all())
