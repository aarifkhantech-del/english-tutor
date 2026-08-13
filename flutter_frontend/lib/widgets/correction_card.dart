import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../theme/app_colors.dart';
import '../models/correction_result.dart';

class CorrectionCard extends StatelessWidget {
  final CorrectionResult correction;
  final bool isPlayingAudio;
  final VoidCallback onPlayAudio;

  const CorrectionCard({
    super.key,
    required this.correction,
    required this.isPlayingAudio,
    required this.onPlayAudio,
  });

  void _copyToClipboard(BuildContext context) {
    Clipboard.setData(ClipboardData(text: correction.corrected));
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Copied corrected sentence to clipboard!'),
        duration: Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: AppColors.primaryLight.withOpacity(0.5),
          width: 1.5,
        ),
        boxShadow: [
          BoxShadow(
            color: AppColors.primary.withOpacity(0.15),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header Badge, Copy Button & Play Audio Button
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: AppColors.primary.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: const Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.auto_awesome_rounded,
                      color: AppColors.primaryLight,
                      size: 16,
                    ),
                    SizedBox(width: 6),
                    Text(
                      'Corrected English',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                        color: AppColors.primaryLight,
                      ),
                    ),
                  ],
                ),
              ),

              Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.copy_rounded, size: 20, color: AppColors.textSecondary),
                    tooltip: 'Copy sentence',
                    onPressed: () => _copyToClipboard(context),
                  ),
                  const SizedBox(width: 4),
                  ElevatedButton.icon(
                    onPressed: onPlayAudio,
                    icon: Icon(
                      isPlayingAudio ? Icons.pause_circle_filled : Icons.volume_up_rounded,
                      size: 20,
                    ),
                    label: Text(isPlayingAudio ? 'Listening...' : 'Listen'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),

          const SizedBox(height: 16),

          // Corrected Sentence Text
          Text(
            correction.corrected,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: AppColors.textPrimary,
              height: 1.3,
            ),
          ),

          if (correction.explanation.isNotEmpty) ...[
            const SizedBox(height: 16),
            const Divider(color: Color(0xFF334155)),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(
                  Icons.lightbulb_outline_rounded,
                  color: AppColors.secondary,
                  size: 20,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Why this correction?',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: AppColors.secondary,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        correction.explanation,
                        style: const TextStyle(
                          fontSize: 14,
                          color: AppColors.textSecondary,
                          height: 1.4,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}
