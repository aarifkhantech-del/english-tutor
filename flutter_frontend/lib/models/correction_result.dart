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
    final translation = json['english_translation'] as String? ?? json['corrected'] as String? ?? '';
    return CorrectionResult(
      hindiInput: json['hindi_input'] as String? ?? '',
      englishTranslation: translation,
      corrected: translation,
      explanation: json['explanation'] as String? ?? '',
      practice: json['practice'] as String? ?? '',
      encouragement: json['encouragement'] as String? ?? '',
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
