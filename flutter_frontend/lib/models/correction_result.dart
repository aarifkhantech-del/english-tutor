/// Safely extracts a String from a dynamic value.
String _s(dynamic v, [String fallback = '']) {
  if (v == null) return fallback;
  if (v is String) return v;
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
    final translation = _s(json['english_translation'].isEmpty == true
        ? json['corrected']
        : json['english_translation']);
    // Prefer english_translation, fall back to corrected
    final eng = _s(json['english_translation']).isNotEmpty
        ? _s(json['english_translation'])
        : _s(json['corrected']);

    return CorrectionResult(
      hindiInput: _s(json['hindi_input']),
      englishTranslation: eng,
      corrected: eng,
      explanation: _s(json['explanation']),
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
