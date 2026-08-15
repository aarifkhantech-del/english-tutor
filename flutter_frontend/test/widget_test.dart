import 'package:flutter_test/flutter_test.dart';
import 'package:english_tutor/main.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const EnglishTutorApp());
    expect(find.byType(EnglishTutorApp), findsOneWidget);
  });
}
