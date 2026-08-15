import 'dart:convert';
import 'dart:typed_data';
import 'package:audioplayers/audioplayers.dart';
import '../config/api_config.dart';

class AudioPlayerService {
  final AudioPlayer _player = AudioPlayer();

  AudioPlayer get player => _player;

  /// Plays audio from a base64 encoded string directly from memory
  Future<void> playBase64(String base64String) async {
    if (base64String.trim().isEmpty) return;
    try {
      final Uint8List bytes = base64Decode(base64String.trim());
      await _player.stop();
      await _player.play(BytesSource(bytes, mimeType: 'audio/mpeg'));
    } catch (e) {
      print('AudioPlayerService playBase64 error: $e');
    }
  }

  /// Plays audio from raw byte array directly from memory
  Future<void> playBytes(Uint8List bytes) async {
    if (bytes.isEmpty) return;
    await _player.stop();
    await _player.play(BytesSource(bytes, mimeType: 'audio/mpeg'));
  }

  /// Plays audio from relative or absolute URL (fallback/legacy)
  Future<void> playAudio(String relativeOrAbsoluteUrl) async {
    if (relativeOrAbsoluteUrl.trim().isEmpty) return;
    final fullUrl = ApiConfig.getAudioUrl(relativeOrAbsoluteUrl);
    await _player.stop();
    await _player.play(UrlSource(fullUrl));
  }

  /// Stops current playback
  Future<void> stop() async {
    await _player.stop();
  }

  /// Pause playback
  Future<void> pause() async {
    await _player.pause();
  }

  /// Resume playback
  Future<void> resume() async {
    await _player.resume();
  }

  /// Dispose resources
  void dispose() {
    _player.dispose();
  }
}
