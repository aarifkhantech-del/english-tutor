import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

class TopicPrompt {
  final String emoji;
  final String title;
  final String sampleSentence;

  const TopicPrompt({
    required this.emoji,
    required this.title,
    required this.sampleSentence,
  });
}

class PracticeTopicsBar extends StatelessWidget {
  final ValueChanged<String> onSelectPrompt;

  const PracticeTopicsBar({
    super.key,
    required this.onSelectPrompt,
  });

  static const List<TopicPrompt> topics = [
    TopicPrompt(
      emoji: '👋',
      title: 'परिचय',
      sampleSentence: 'नमस्ते, मेरा नाम अमित है और मैं दिल्ली में रहता हूँ।',
    ),
    TopicPrompt(
      emoji: '☕',
      title: 'चाय / कॉफी',
      sampleSentence: 'मुझे एक कप गर्म चाय चाहिए।',
    ),
    TopicPrompt(
      emoji: '💼',
      title: 'नौकरी',
      sampleSentence: 'मैं एक सॉफ्टवेयर डेवलपर हूँ और मुझे तीन साल का अनुभव है।',
    ),
    TopicPrompt(
      emoji: '✈️',
      title: 'रास्ता पूछना',
      sampleSentence: 'क्या आप मुझे नजदीकी बस स्टेशन का रास्ता बता सकते हैं?',
    ),
    TopicPrompt(
      emoji: '🛒',
      title: 'बाजार',
      sampleSentence: 'मैं बाजार से सब्जियाँ और फल खरीदने जा रहा हूँ।',
    ),
    TopicPrompt(
      emoji: '🎓',
      title: 'शिक्षा',
      sampleSentence: 'मैं अंग्रेजी सीखना चाहता हूँ ताकि मैं बेहतर नौकरी पा सकूँ।',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Padding(
          padding: EdgeInsets.symmetric(horizontal: 4),
          child: Row(
            children: [
              Icon(Icons.tips_and_updates_outlined, color: AppColors.secondary, size: 18),
              SizedBox(width: 6),
              Text(
                'Hindi में बोलकर शुरू करें',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 10),
        SizedBox(
          height: 40,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            itemCount: topics.length,
            separatorBuilder: (_, __) => const SizedBox(width: 8),
            itemBuilder: (context, index) {
              final topic = topics[index];
              return ActionChip(
                backgroundColor: AppColors.surface,
                side: BorderSide(color: AppColors.textMuted.withOpacity(0.3)),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                label: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(topic.emoji, style: const TextStyle(fontSize: 14)),
                    const SizedBox(width: 6),
                    Text(
                      topic.title,
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppColors.textPrimary,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
                onPressed: () => onSelectPrompt(topic.sampleSentence),
              );
            },
          ),
        ),
      ],
    );
  }
}
