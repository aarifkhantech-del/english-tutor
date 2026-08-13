# 🗣️ English Tutor AI - Flutter Application

A modern, voice-powered cross-platform Flutter application for practicing English speaking skills with instant AI corrections, grammar breakdowns, practice prompts, and audio pronunciation feedback.

---

## 🌟 Key Features

- 🎤 **Voice Recording**: One-tap interactive microphone button with pulse animations during recording.
- ⚡ **Real-Time AI Processing**: Connects to the FastAPI backend (`Whisper` speech recognition + `Mistral 7B` grammar model + `Piper` TTS engine).
- 💡 **Grammar & Pronunciation Breakdown**:
  - Displays what you said.
  - Highlights corrected English sentence.
  - Explains *why* the grammar or phrasing was updated.
  - Recommends a practice sentence.
  - Delivers an encouraging message.
- 🔊 **Audio Pronunciation Playback**: Tap to listen to the generated TTS voice model pronouncing the corrected sentence.
- ⚙️ **Dynamic Server Config**: In-app Server Settings dialog to switch between `http://127.0.0.1:8000` (Web/Desktop), `http://10.0.2.2:8000` (Android Emulator), or custom local IP addresses.

---

## 📁 Project Structure

```
flutter_frontend/
├── pubspec.yaml
├── README.md
├── android/
│   └── app/src/main/AndroidManifest.xml
├── ios/
│   └── Runner/Info.plist
└── lib/
    ├── main.dart                      # App entry point
    ├── config/
    │   └── api_config.dart            # Backend endpoint config & preferences
    ├── models/
    │   ├── correction_result.dart     # Parsed LLM grammar feedback model
    │   └── tutor_response.dart        # Full API response model
    ├── services/
    │   ├── api_service.dart           # Multipart audio upload to /tutor
    │   ├── audio_recorder_service.dart# Microphone recording service
    │   └── audio_player_service.dart  # TTS playback service
    ├── theme/
    │   ├── app_colors.dart            # Modern dark color palette
    │   └── app_theme.dart             # ThemeData with GoogleFonts
    ├── widgets/
    │   ├── record_button.dart         # Pulse animated mic button
    │   ├── transcription_card.dart    # User speech card
    │   ├── correction_card.dart       # Corrected sentence & audio button
    │   ├── practice_section.dart      # Practice prompt & encouragement
    │   └── server_settings_dialog.dart# Backend URL configuration dialog
    └── screens/
        └── tutor_home_screen.dart     # Main interactive app view
```

---

## 🚀 How to Run the App

### 1. Start the FastAPI Backend
Ensure the FastAPI backend is running on your machine:
```bash
cd backend
python main.py
```
*(Backend runs at `http://127.0.0.1:8000`)*

### 2. Install Flutter Dependencies
Navigate into `flutter_frontend` and fetch packages:
```bash
cd flutter_frontend
flutter pub get
```

### 3. Run on your preferred target platform

- **Android Emulator**:
  ```bash
  flutter run -d android
  ```
  *(Default server URL `http://10.0.2.2:8000` will connect seamlessly to your host machine)*

- **Chrome / Web**:
  ```bash
  flutter run -d chrome
  ```

- **Windows Desktop**:
  ```bash
  flutter run -d windows
  ```

- **iOS Simulator**:
  ```bash
  flutter run -d iphone
  ```

---

## 🛠️ Setting Up Flutter SDK (If not already installed)

1. Download Flutter SDK for Windows from [flutter.dev](https://docs.flutter.dev/get-started/install/windows/mobile).
2. Extract to a directory (e.g. `C:\src\flutter`).
3. Add `C:\src\flutter\bin` to your System Environment `PATH`.
4. Run `flutter doctor` in PowerShell to verify setup.
