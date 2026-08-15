import 'package:audioplayers/audioplayers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../config/api_config.dart';
import '../models/practice_session.dart';
import '../models/tutor_response.dart';
import '../services/api_service.dart';
import '../services/audio_player_service.dart';
import '../services/audio_recorder_service.dart';
import '../services/history_service.dart';
import '../theme/app_colors.dart';
import '../widgets/correction_card.dart';
import '../widgets/history_bottom_sheet.dart';
import '../widgets/practice_section.dart';
import '../widgets/practice_topics_bar.dart';
import '../widgets/record_button.dart';
import '../widgets/server_settings_dialog.dart';
import '../widgets/transcription_card.dart';

class TutorHomeScreen extends StatefulWidget {
  const TutorHomeScreen({super.key});

  @override
  State<TutorHomeScreen> createState() => _TutorHomeScreenState();
}

class _TutorHomeScreenState extends State<TutorHomeScreen> {
  final AudioRecorderService _recorderService = AudioRecorderService();
  final AudioPlayerService _playerService = AudioPlayerService();

  bool _isRecording = false;
  bool _isProcessing = false;
  bool _isPlayingAudio = false;

  TutorResponse? _result;
  String? _errorMessage;
  bool _isServerOnline = true;

  @override
  void initState() {
    super.initState();
    _checkServerHealth();
    _listenToAudioState();
  }

  void _listenToAudioState() {
    _playerService.player.onPlayerStateChanged.listen((state) {
      if (mounted) {
        setState(() {
          _isPlayingAudio = state == PlayerState.playing;
        });
      }
    });
  }

  Future<void> _checkServerHealth() async {
    final healthy = await ApiService.checkHealth();
    if (mounted) {
      setState(() {
        _isServerOnline = healthy;
      });
    }
  }

  Future<void> _toggleRecording() async {
    setState(() {
      _errorMessage = null;
    });

    if (_isRecording) {
      // Stop recording
      try {
        final path = await _recorderService.stopRecording();
        setState(() {
          _isRecording = false;
        });

        if (path != null) {
          await _processAudio(path);
        }
      } catch (e) {
        setState(() {
          _isRecording = false;
          _errorMessage = 'Failed to stop recording: $e';
        });
      }
    } else {
      // Start recording
      try {
        await _recorderService.startRecording();
        setState(() {
          _isRecording = true;
          _result = null;
        });
      } catch (e) {
        setState(() {
          _errorMessage = 'Microphone error: $e';
        });
      }
    }
  }

  Future<void> _processAudio(String filePath) async {
    setState(() {
      _isProcessing = true;
    });

    try {
      final response = await ApiService.submitAudio(filePath);
      setState(() {
        _result = response;
        _isProcessing = false;
      });

      // Save to local practice history
      await HistoryService.saveSession(
        PracticeSession(
          id: DateTime.now().millisecondsSinceEpoch.toString(),
          transcription: response.transcription,
          correction: response.correction,
          timestamp: DateTime.now(),
        ),
      );

      // Auto play pronunciation TTS response (in-memory base64 or URL fallback)
      if (response.audioB64.isNotEmpty) {
        await _playerService.playBase64(response.audioB64);
      } else if (response.audioUrl.isNotEmpty) {
        await _playerService.playAudio(response.audioUrl);
      }
    } catch (e) {
      setState(() {
        _isProcessing = false;
        _errorMessage = e.toString().replaceAll('Exception: ', '');
      });
    }
  }

  Future<void> _playTutorAudio() async {
    if (_result == null || !_result!.hasAudio) return;

    if (_isPlayingAudio) {
      await _playerService.stop();
    } else {
      if (_result!.audioB64.isNotEmpty) {
        await _playerService.playBase64(_result!.audioB64);
      } else if (_result!.audioUrl.isNotEmpty) {
        await _playerService.playAudio(_result!.audioUrl);
      }
    }
  }

  void _openSettings() async {
    await showDialog(
      context: context,
      builder: (_) => const ServerSettingsDialog(),
    );
    _checkServerHealth();
  }

  void _openHistory() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => const HistoryBottomSheet(),
    );
  }

  void _onSelectTopicPrompt(String sampleSentence) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('🎙️ यह बोलने की कोशिश करें: "$sampleSentence"'),
        action: SnackBarAction(
          label: 'ठीक है',
          onPressed: () {},
        ),
        duration: const Duration(seconds: 5),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  void dispose() {
    _recorderService.dispose();
    _playerService.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              '🇮🇳 Hindi → English',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20),
            ),
            const SizedBox(width: 8),
            Container(
              width: 8,
              height: 8,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _isServerOnline ? AppColors.accent : AppColors.recordingRed,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.history_rounded, color: AppColors.textSecondary),
            tooltip: 'Practice History',
            onPressed: _openHistory,
          ),
          IconButton(
            icon: const Icon(Icons.settings_outlined, color: AppColors.textSecondary),
            tooltip: 'Server Settings',
            onPressed: _openSettings,
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Header Subtitle
              Center(
                child: Text(
                  'Hindi में बोलें, English में सीखें 🎯',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 14,
                    color: AppColors.textSecondary.withOpacity(0.9),
                  ),
                ),
              ),

              const SizedBox(height: 20),

              // Practice Topics Carousel
              PracticeTopicsBar(onSelectPrompt: _onSelectTopicPrompt),

              const SizedBox(height: 28),

              // Recording Button Widget
              RecordButton(
                isRecording: _isRecording,
                isProcessing: _isProcessing,
                onTap: _toggleRecording,
              ),

              const SizedBox(height: 28),

              // Server Offline Banner Notice
              if (!_isServerOnline && !_isRecording && !_isProcessing) ...[
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.recordingRed.withOpacity(0.12),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: AppColors.recordingRed.withOpacity(0.3)),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.warning_amber_rounded, color: AppColors.recordingRed, size: 20),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          'Server offline. Ensure FastAPI backend is running at ${ApiConfig.baseUrl}',
                          style: const TextStyle(fontSize: 12, color: AppColors.recordingRed),
                        ),
                      ),
                      TextButton(
                        onPressed: _openSettings,
                        child: const Text('Configure'),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
              ],

              // Error banner
              if (_errorMessage != null) ...[
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: AppColors.recordingRed.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: AppColors.recordingRed),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.error_outline_rounded, color: AppColors.recordingRed),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          _errorMessage!,
                          style: const TextStyle(color: AppColors.textPrimary, fontSize: 14),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 24),
              ],

              // Results Section
              if (_result != null) ...[
                // Transcription
                TranscriptionCard(text: _result!.transcription)
                    .animate()
                    .fadeIn(duration: 400.ms)
                    .slideY(begin: 0.1, end: 0.0),

                const SizedBox(height: 20),

                // Correction Card
                CorrectionCard(
                  correction: _result!.correction,
                  isPlayingAudio: _isPlayingAudio,
                  onPlayAudio: _playTutorAudio,
                )
                    .animate()
                    .fadeIn(duration: 500.ms, delay: 150.ms)
                    .slideY(begin: 0.1, end: 0.0),

                const SizedBox(height: 20),

                // Practice & Encouragement
                PracticeSection(correction: _result!.correction)
                    .animate()
                    .fadeIn(duration: 600.ms, delay: 300.ms)
                    .slideY(begin: 0.1, end: 0.0),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
