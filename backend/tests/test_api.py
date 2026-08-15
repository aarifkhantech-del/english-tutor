import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app
from app.services.tts_service import tts_service


@pytest.mark.asyncio
async def test_root_endpoint():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "online"


@pytest.mark.asyncio
async def test_health_endpoint():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert data["tts"] == "configured"


@pytest.mark.asyncio
async def test_in_memory_tts_generation():
    audio_b64 = await tts_service.generate_audio_b64("Hello, welcome to English tutor!")
    assert isinstance(audio_b64, str)
    assert len(audio_b64) > 100
