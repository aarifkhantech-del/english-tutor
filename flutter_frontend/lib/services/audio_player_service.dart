import 'package:audioplayers/audioplayers.dart';
import '../config/api_config.dart';

class AudioPlayerService {
  final AudioPlayer _player = AudioPlayer();

  AudioPlayer get player => _player;

  /// Plays audio from relative or absolute URL
  Future<void> playAudio(String relativeOrAbsoluteUrl) async {
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
