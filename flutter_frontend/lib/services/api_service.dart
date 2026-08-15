import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';
import '../models/tutor_response.dart';

class ApiService {
  /// Sends audio file (file path on native, blob URL on web) to backend /tutor endpoint
  static Future<TutorResponse> submitAudio(String filePathOrBlobUrl) async {
    final uri = ApiConfig.tutorEndpoint;
    final request = http.MultipartRequest('POST', uri);

    if (kIsWeb) {
      // On web, fetch bytes from blob URL
      final response = await http.get(Uri.parse(filePathOrBlobUrl));
      final bytes = response.bodyBytes;
      final multipartFile = http.MultipartFile.fromBytes(
        'file',
        bytes,
        filename: 'recording_${DateTime.now().millisecondsSinceEpoch}.webm',
      );
      request.files.add(multipartFile);
    } else {
      // On native (Android, iOS, Windows, macOS, Linux)
      final multipartFile = await http.MultipartFile.fromPath(
        'file',
        filePathOrBlobUrl,
      );
      request.files.add(multipartFile);
    }

    try {
      final streamedResponse = await request.send().timeout(
        const Duration(seconds: 120),
      );
      final response = await http.Response.fromStream(streamedResponse);

      if (response.statusCode == 200) {
        final decoded = json.decode(utf8.decode(response.bodyBytes));
        return TutorResponse.fromJson(decoded);
      } else {
        throw Exception('Server returned status code ${response.statusCode}: ${response.body}');
      }
    } catch (e) {
      throw Exception('Failed to connect to English Tutor backend: $e');
    }
  }

  /// Checks backend health status
  static Future<bool> checkHealth() async {
    try {
      final response = await http.get(ApiConfig.healthEndpoint).timeout(
        const Duration(seconds: 5),
      );
      return response.statusCode == 200;
    } catch (_) {
      return false;
    }
  }
}
