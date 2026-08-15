import 'dart:convert';
import 'dart:typed_data';
import 'correction_result.dart';

/// Safely extracts a String from a dynamic value.
/// If the value is already a String, returns it.
/// If it's a Map or List, converts to JSON-like string.
/// Returns [fallback] for null.
String _safeString(dynamic value, [String fallback = '']) {
  if (value == null) return fallback;
  if (value is String) return value;
  return value.toString();
}

class TutorResponse {
  final String transcription;
  final CorrectionResult correction;
  final String audioB64;
  final String audioUrl;

  TutorResponse({
    required this.transcription,
    required this.correction,
    this.audioB64 = '',
    this.audioUrl = '',
  });

  bool get hasAudio => audioB64.isNotEmpty || audioUrl.isNotEmpty;

  Uint8List? get audioBytes {
    if (audioB64.isEmpty) return null;
    try {
      return base64Decode(audioB64);
    } catch (_) {
      return null;
    }
  }

  factory TutorResponse.fromJson(Map<String, dynamic> json) {
    final correctionRaw = json['correction'];
    CorrectionResult correctionObj;

    if (correctionRaw is Map<String, dynamic>) {
      correctionObj = CorrectionResult.fromJson(correctionRaw);
    } else if (correctionRaw is String) {
      correctionObj = CorrectionResult(
        hindiInput: '',
        englishTranslation: correctionRaw,
        corrected: correctionRaw,
        explanation: '',
        practice: '',
        encouragement: '',
      );
    } else {
      final fallback = _safeString(json['transcription']);
      correctionObj = CorrectionResult(
        hindiInput: fallback,
        englishTranslation: fallback,
        corrected: fallback,
        explanation: '',
        practice: '',
        encouragement: '',
      );
    }

    return TutorResponse(
      transcription: _safeString(json['transcription']),
      correction: correctionObj,
      audioB64: _safeString(json['audio_b64']),
      audioUrl: _safeString(json['audio_url']),
    );
  }
}
