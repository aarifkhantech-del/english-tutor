import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/practice_session.dart';

class HistoryService {
  static const String _keyHistory = 'practice_history_list';

  /// Saves a new practice session to local storage
  static Future<void> saveSession(PracticeSession session) async {
    final prefs = await SharedPreferences.getInstance();
    final history = await getHistory();
    history.insert(0, session);

    // Keep up to 50 practice entries
    if (history.length > 50) {
      history.removeRange(50, history.length);
    }

    final encodedList = history.map((s) => s.encode()).toList();
    await prefs.setStringList(_keyHistory, encodedList);
  }

  /// Retrieves practice history list
  static Future<List<PracticeSession>> getHistory() async {
    final prefs = await SharedPreferences.getInstance();
    final rawList = prefs.getStringList(_keyHistory) ?? [];
    return rawList.map((str) {
      try {
        return PracticeSession.decode(str);
      } catch (_) {
        return null;
      }
    }).whereType<PracticeSession>().toList();
  }

  /// Clears all practice history
  static Future<void> clearHistory() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keyHistory);
  }
}
