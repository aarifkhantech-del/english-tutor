class CorrectionResult {
  final String corrected;
  final String explanation;
  final String practice;
  final String encouragement;

  CorrectionResult({
    required this.corrected,
    required this.explanation,
    required this.practice,
    required this.encouragement,
  });

  factory CorrectionResult.fromJson(Map<String, dynamic> json) {
    return CorrectionResult(
      corrected: json['corrected'] as String? ?? '',
      explanation: json['explanation'] as String? ?? '',
      practice: json['practice'] as String? ?? '',
      encouragement: json['encouragement'] as String? ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'corrected': corrected,
      'explanation': explanation,
      'practice': practice,
      'encouragement': encouragement,
    };
  }
}
