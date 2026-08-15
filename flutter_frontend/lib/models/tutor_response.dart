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
  final String audioUrl;

  TutorResponse({
    required this.transcription,
    required this.correction,
    required this.audioUrl,
  });

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
      audioUrl: _safeString(json['audio_url']),
    );
  }
}
