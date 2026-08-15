/// Safely extracts a String from a dynamic value.
String _s(dynamic v, [String fallback = '']) {
  if (v == null) return fallback;
  if (v is String) return v;
  return v.toString();
}

/// Flattens explanation field whether it's a String or a nested Map.
String _formatExplanation(dynamic v) {
  if (v == null) return '';
  if (v is String) return v;
  if (v is Map) {
    final parts = <String>[];
    v.forEach((key, value) {
      if (value is Map) {
        value.forEach((k2, v2) => parts.add('$k2: $v2'));
      } else {
        parts.add('$value');
      }
    });
    return parts.join(' • ');
  }
  return v.toString();
}

class CorrectionResult {
  final String hindiInput;
  final String englishTranslation;
  final String corrected;
  final String explanation;
  final String practice;
  final String encouragement;

  CorrectionResult({
    required this.hindiInput,
    required this.englishTranslation,
    required this.corrected,
    required this.explanation,
    required this.practice,
    required this.encouragement,
  });

  factory CorrectionResult.fromJson(Map<String, dynamic> json) {
    // Prefer english_translation, fall back to corrected
    final eng = _s(json['english_translation']).isNotEmpty
        ? _s(json['english_translation'])
        : _s(json['corrected']);

    return CorrectionResult(
      hindiInput: _s(json['hindi_input']),
      englishTranslation: eng,
      corrected: eng,
      explanation: _formatExplanation(json['explanation']),
      practice: _s(json['practice']),
      encouragement: _s(json['encouragement']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'hindi_input': hindiInput,
      'english_translation': englishTranslation,
      'corrected': corrected,
      'explanation': explanation,
      'practice': practice,
      'encouragement': encouragement,
    };
  }
}
