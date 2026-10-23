import 'dart:convert';

import 'package:contaplus/core/api_client.dart';
import 'package:contaplus/core/token_store.dart';
import 'package:contaplus/main.dart';
import 'package:contaplus/services/auth_service.dart';
import 'package:contaplus/session/session_controller.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

class MemoryTokenStore implements TokenStore {
  MemoryTokenStore([this.token]);

  String? token;

  @override
  Future<String?> read() async => token;

  @override
  Future<void> write(String token) async => this.token = token;

  @override
  Future<void> clear() async => token = null;
}

const _usuario = {
  'id': '0edf2102-4536-40ec-b6d4-7226986ec8a4',
  'email': 'teste@contaplus.dev',
  'name': 'Eduardo Teste',
  'cnpj': null,
  'role': 'OWNER',
  'storeId': 'a4d53bdd-2693-4f4d-a32d-4f2ebd38f685',
  'storeName': 'Loja Teste',
};

http.Response _json(Object body, [int status = 200]) =>
    http.Response(jsonEncode(body), status, headers: {'content-type': 'application/json'});

Future<void> _abrirApp(
  WidgetTester tester, {
  required MemoryTokenStore store,
  required MockClientHandler api,
}) async {
  final client = ApiClient(httpClient: MockClient(api), baseUrl: 'http://api.test');
  final session = SessionController(api: client, auth: AuthService(client), tokenStore: store);
  await tester.pumpWidget(ContaPlusApp(session: session));
  await tester.pumpAndSettle();
}

Future<void> _entrarCom(WidgetTester tester, String email, String senha) async {
  await tester.enterText(find.byType(TextField).at(0), email);
  await tester.enterText(find.byType(TextField).at(1), senha);
  await tester.tap(find.text('Entrar'));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('sem token salvo, abre no login sem chamar a API', (tester) async {
    var chamadas = 0;
    await _abrirApp(tester, store: MemoryTokenStore(), api: (_) async {
      chamadas++;
      return http.Response('', 500);
    });

    expect(find.text('Entre na sua conta'), findsOneWidget);
    expect(chamadas, 0);
  });

  testWidgets('com token aceito pela API, abre direto na home', (tester) async {
    final store = MemoryTokenStore('token-valido');
    http.Request? recebida;
    await _abrirApp(tester, store: store, api: (req) async {
      recebida = req;
      return _json(_usuario);
    });

    expect(find.text('Olá, Eduardo'), findsOneWidget);
    expect(recebida!.url.path, '/v1/auth/me');
    expect(recebida!.headers['Authorization'], 'Bearer token-valido');
    expect(store.token, 'token-valido');
  });

  testWidgets('token recusado (401) é apagado e volta pro login', (tester) async {
    final store = MemoryTokenStore('token-expirado');
    await _abrirApp(tester, store: store, api: (_) async => http.Response('', 401));

    expect(find.text('Entre na sua conta'), findsOneWidget);
    expect(store.token, isNull);
  });

  testWidgets('sem servidor, mantém o token e deixa tentar de novo', (tester) async {
    final store = MemoryTokenStore('token-valido');
    var servidorNoAr = false;
    await _abrirApp(tester, store: store, api: (_) async {
      if (!servidorNoAr) throw http.ClientException('Connection refused');
      return _json(_usuario);
    });

    expect(find.text('Sem conexão com o servidor'), findsOneWidget);
    expect(store.token, 'token-valido');

    servidorNoAr = true;
    await tester.tap(find.text('Tentar de novo'));
    await tester.pumpAndSettle();

    expect(find.text('Olá, Eduardo'), findsOneWidget);
  });

  testWidgets('login salva o token e abre a home', (tester) async {
    final store = MemoryTokenStore();
    http.Request? recebida;
    await _abrirApp(tester, store: store, api: (req) async {
      recebida = req;
      return _json({'token': 'token-novo', 'user': _usuario});
    });

    await _entrarCom(tester, 'teste@contaplus.dev', 'Teste123');

    expect(find.text('Olá, Eduardo'), findsOneWidget);
    expect(recebida!.url.path, '/v1/auth/login');
    expect(jsonDecode(recebida!.body), {'email': 'teste@contaplus.dev', 'password': 'Teste123'});
    expect(store.token, 'token-novo');
  });

  testWidgets('senha errada mostra o erro e não guarda token', (tester) async {
    final store = MemoryTokenStore();
    await _abrirApp(tester, store: store, api: (_) async {
      return _json({'status': 401, 'message': 'Credenciais inválidas'}, 401);
    });

    await _entrarCom(tester, 'teste@contaplus.dev', 'errada');

    expect(find.text('Email ou senha inválidos.'), findsOneWidget);
    expect(find.text('Entre na sua conta'), findsOneWidget);
    expect(store.token, isNull);

    // Deixa o SnackBar expirar para não sobrar timer pendente no fim do teste.
    await tester.pump(const Duration(seconds: 5));
    await tester.pumpAndSettle();
  });

  testWidgets('sair apaga o token e volta pro login', (tester) async {
    final store = MemoryTokenStore('token-valido');
    await _abrirApp(tester, store: store, api: (_) async => _json(_usuario));

    await tester.tap(find.byTooltip('Sair'));
    await tester.pumpAndSettle();

    expect(find.text('Entre na sua conta'), findsOneWidget);
    expect(store.token, isNull);
  });
}
