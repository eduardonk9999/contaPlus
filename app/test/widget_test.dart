// Smoke test da tela de login do Conta+.

import 'package:flutter_test/flutter_test.dart';

import 'package:contaplus/main.dart';

void main() {
  testWidgets('A tela de login renderiza', (WidgetTester tester) async {
    await tester.pumpWidget(const ContaPlusApp());

    // Elementos esperados na tela inicial (login).
    expect(find.text('Conta+'), findsOneWidget);
    expect(find.text('Entrar'), findsOneWidget);
    expect(find.text('Criar conta'), findsOneWidget);
  });
}
