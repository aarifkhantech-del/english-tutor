import 'dart:convert';
import 'correction_result.dart';

class PracticeSession {
  final String id;
  final String transcription;
  final CorrectionResult correction;
  final DateTime timestamp;

  PracticeSession({
    required this.id,
    required this.transcription,
    required this.correction,
    required this.timestamp,
  });

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'transcription': transcription,
      'correction': correction.toJson(),
      'timestamp': timestamp.toIso8601String(),
    };
  }

  factory PracticeSession.fromJson(Map<String, dynamic> json) {
    return PracticeSession(
      id: json['id'] as String? ?? DateTime.now().millisecondsSinceEpoch.toString(),
      transcription: json['transcription'] as String? ?? '',
      correction: CorrectionResult.fromJson(
        json['correction'] as Map<String, dynamic>? ?? {},
      ),
      timestamp: DateTime.tryParse(json['timestamp'] as String? ?? '') ?? DateTime.now(),
    );
  }

  String encode() => json.encode(toJson());
  factory PracticeSession.decode(String str) => PracticeSession.fromJson(json.decode(str));
}
