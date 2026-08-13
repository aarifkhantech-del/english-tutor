import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../theme/app_colors.dart';

class RecordButton extends StatelessWidget {
  final bool isRecording;
  final bool isProcessing;
  final VoidCallback onTap;

  const RecordButton({
    super.key,
    required this.isRecording,
    required this.isProcessing,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final buttonColor = isRecording ? AppColors.recordingRed : AppColors.primary;

    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          GestureDetector(
            onTap: isProcessing ? null : onTap,
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 300),
              height: isRecording ? 100 : 90,
              width: isRecording ? 100 : 90,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: buttonColor,
                boxShadow: [
                  BoxShadow(
                    color: isRecording
                        ? AppColors.recordingGlow
                        : AppColors.primary.withOpacity(0.4),
                    blurRadius: isRecording ? 30 : 16,
                    spreadRadius: isRecording ? 8 : 4,
                  ),
                ],
              ),
              child: Center(
                child: isProcessing
                    ? const SizedBox(
                        height: 36,
                        width: 36,
                        child: CircularProgressIndicator(
                          color: Colors.white,
                          strokeWidth: 3,
                        ),
                      )
                    : Icon(
                        isRecording ? Icons.stop_rounded : Icons.mic_rounded,
                        color: Colors.white,
                        size: isRecording ? 44 : 40,
                      ),
              ),
            ).animate(target: isRecording ? 1 : 0).scale(
                  begin: const Offset(1.0, 1.0),
                  end: const Offset(1.08, 1.08),
                  duration: 800.ms,
                  curve: Curves.easeInOut,
                ),
          ),
          const SizedBox(height: 16),
          Text(
            isProcessing
                ? 'AI is reviewing your speech...'
                : isRecording
                    ? 'Tap to stop speaking'
                    : 'Tap microphone to speak',
            style: TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w500,
              color: isRecording ? AppColors.recordingRed : AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}
