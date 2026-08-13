import 'package:flutter/material.dart';
import 'config/api_config.dart';
import 'screens/tutor_home_screen.dart';
import 'theme/app_theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await ApiConfig.init();
  runApp(const EnglishTutorApp());
}

class EnglishTutorApp extends StatelessWidget {
  const EnglishTutorApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'English Tutor AI',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.darkTheme,
      home: const TutorHomeScreen(),
    );
  }
}
