import 'package:shared_preferences/shared_preferences.dart';

class ApiConfig {
  static const String _keyBaseUrl = 'backend_base_url';

  // Default backend URL (Render.com production):
  // static const String defaultUrl = 'http://127.0.0.1:8000';
  static const String defaultUrl = 'https://english-tutor-6fx2.onrender.com';

  static String _currentBaseUrl = defaultUrl;

  static String get baseUrl => _currentBaseUrl;

  static Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    _currentBaseUrl = prefs.getString(_keyBaseUrl) ?? defaultUrl;
  }

  static Future<void> setBaseUrl(String newUrl) async {
    var formatted = newUrl.trim();
    if (formatted.endsWith('/')) {
      formatted = formatted.substring(0, formatted.length - 1);
    }
    _currentBaseUrl = formatted;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyBaseUrl, formatted);
  }

  static Uri get tutorEndpoint => Uri.parse('$_currentBaseUrl/tutor');
  static Uri get healthEndpoint => Uri.parse('$_currentBaseUrl/health');
  static Uri get transcribeEndpoint => Uri.parse('$_currentBaseUrl/transcribe');

  static String getAudioUrl(String relativePath) {
    if (relativePath.startsWith('http://') || relativePath.startsWith('https://')) {
      return relativePath;
    }
    final cleanPath = relativePath.startsWith('/') ? relativePath : '/$relativePath';
    return '$_currentBaseUrl$cleanPath';
  }
}
