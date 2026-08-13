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
      title: 'Introduction',
      sampleSentence: 'Hello! My name is Alex, and I am learning English.',
    ),
    TopicPrompt(
      emoji: '☕',
      title: 'Order Coffee',
      sampleSentence: 'Can I get a hot cappuccino with oat milk, please?',
    ),
    TopicPrompt(
      emoji: '💼',
      title: 'Job Interview',
      sampleSentence: 'I have three years of experience in software development.',
    ),
    TopicPrompt(
      emoji: '✈️',
      title: 'Travel Advice',
      sampleSentence: 'Excuse me, could you tell me where the nearest station is?',
    ),
    TopicPrompt(
      emoji: '🎨',
      title: 'Hobbies',
      sampleSentence: 'In my free time, I really enjoy playing guitar and reading books.',
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
                'Practice Topics',
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
