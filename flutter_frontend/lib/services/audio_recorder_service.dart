import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:record/record.dart';

class AudioRecorderService {
  final AudioRecorder _audioRecorder = AudioRecorder();
  bool _isRecording = false;

  bool get isRecording => _isRecording;

  /// Requests microphone permission on mobile/desktop/web platforms
  Future<bool> hasPermission() async {
    if (!kIsWeb && (defaultTargetPlatform == TargetPlatform.android || defaultTargetPlatform == TargetPlatform.iOS)) {
      final status = await Permission.microphone.request();
      return status.isGranted;
    }
    return await _audioRecorder.hasPermission();
  }

  /// Starts audio recording
  Future<void> startRecording() async {
    final granted = await hasPermission();
    if (!granted) {
      throw Exception('Microphone permission was denied.');
    }

    String? path;
    if (!kIsWeb) {
      final tempDir = await getTemporaryDirectory();
      path = '${tempDir.path}/recording_${DateTime.now().millisecondsSinceEpoch}.m4a';
    }

    const config = RecordConfig(
      encoder: AudioEncoder.aacLc,
      sampleRate: 44100,
      bitRate: 128000,
    );

    await _audioRecorder.start(config, path: path ?? '');
    _isRecording = true;
  }

  /// Stops recording and returns local file path or blob URL
  Future<String?> stopRecording() async {
    if (!_isRecording) return null;
    final path = await _audioRecorder.stop();
    _isRecording = false;
    return path;
  }

  /// Dispose recorder resources
  Future<void> dispose() async {
    await _audioRecorder.dispose();
  }
}
