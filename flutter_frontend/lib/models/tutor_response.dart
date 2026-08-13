import 'correction_result.dart';

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
        corrected: correctionRaw,
        explanation: '',
        practice: '',
        encouragement: '',
      );
    } else {
      correctionObj = CorrectionResult(
        corrected: json['transcription'] as String? ?? '',
        explanation: '',
        practice: '',
        encouragement: '',
      );
    }

    return TutorResponse(
      transcription: json['transcription'] as String? ?? '',
      correction: correctionObj,
      audioUrl: json['audio_url'] as String? ?? '',
    );
  }
}
