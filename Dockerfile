# Root-level Dockerfile for Render.com deployment
# Builds only the FastAPI backend from the backend/ subfolder

FROM python:3.11-slim

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    DEBIAN_FRONTEND=noninteractive

# Install system dependencies (espeak-ng for Piper TTS, ffmpeg for audio)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    ffmpeg \
    espeak-ng \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy requirements first for Docker layer caching
COPY backend/requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy all backend application files
COPY backend/ .

# Create directories for temp uploads and generated audio
RUN mkdir -p uploads audio

# Pre-download Piper TTS voice model from Hugging Face during build
# This avoids cold-start delays on first request
RUN curl -L -o en_US-lessac-medium.onnx \
    "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium.onnx" && \
    curl -L -o en_US-lessac-medium.onnx.json \
    "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium.onnx.json"

# Expose FastAPI port
EXPOSE 8000

# Start Uvicorn server (uses $PORT from Render environment)
CMD uvicorn main:app --host 0.0.0.0 --port ${PORT:-8000}
