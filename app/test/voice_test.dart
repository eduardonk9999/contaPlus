import 'dart:convert';

import 'package:contaplus/app_shell.dart';
import 'package:contaplus/core/api_client.dart';
import 'package:contaplus/models/auth_models.dart';
import 'package:contaplus/theme.dart';
import 'package:contaplus/voice/speech.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

typedef Rota = http.Response Function(http.Request req);

http.Response _json(Object? body, [int status = 200]) => http.Response(
  jsonEncode(body),
  status,
  headers: {'content-type': 'application/json'},
);

Map<String, dynamic> _corpo(http.Request req) =>
    jsonDecode(req.body) as Map<String, dynamic>;

/// Microfone falso: [frase] null = aparelho sem reconhecimento de fala.
class _MicrofoneFalso implements Reconhecedor {
  _MicrofoneFalso([this.frase]);

  final String? frase;

  @override
  Future<bool> preparar() async => frase != null;

  @override
  Future<void> ouvir({
    required void Function(String texto, bool terminou) aoReconhecer,
    required void Function() aoParar,
  }) async {
    aoReconhecer(frase!, true);
  }

  @override
  Future<void> parar() async {}
}

Map<String, dynamic> _interpretacao(
  String tipo,
  String frase, {
  List<Map<String, dynamic>> itens = const [],
  List<String> naoEncontrados = const [],
  String? formaId,
  int? valorPago,
  Map<String, dynamic>? despesa,
  String? mensagem,
}) => {
  'tipo': tipo,
  'frase': frase,
  'itens': itens,
  'naoEncontrados': naoEncontrados,
  'paymentMethodId': formaId,
  'paymentMethodName': null,
  'valorPagoCentavos': valorPago,
  'despesa': despesa,
  'mensagem': mensagem,
};

final _refri = {
  'id': 'p1',
  'storeId': 'loja-1',
  'categoryId': null,
  'name': 'Refrigerante',
  'type': 'SELLABLE',
  'costPriceCents': 300,
  'salePriceCents': 600,
  'stockQuantity': 30,
  'stockUnit': 'UNIT',
  'minStockQuantity': 0,
  'active': true,
};

Map<String, Rota> _rotasBase(Rota interpretar) => {
  'POST /v1/voice/interpret': interpretar,
  'GET /v1/products/p1': (_) => _json(_refri),
  'POST /v1/sales/preview': (req) {
    final qtd = ((_corpo(req)['items'] as List).first['quantity'] as num);
    final total = (600 * qtd).round();
    return _json({
      'items': [],
      'totalAmountCents': total,
      'totalCostCents': 0,
      'grossProfitCents': total ~/ 2,
      'marginPercent': 50,
      'warnings': [],
    });
  },
  'GET /v1/payment-methods': (_) => _json([
    {
      'id': 'm2',
      'name': 'PIX',
      'type': 'PIX',
      'acceptsChange': false,
      'active': true,
    },
    {
      'id': 'm1',
      'name': 'Dinheiro',
      'type': 'CASH',
      'acceptsChange': true,
      'active': true,
    },
  ]),
  'POST /v1/sales': (_) => _json({'id': 'v1', 'totalAmountCents': 1200}, 201),
  'POST /v1/purchases': (req) =>
      _json({'id': 'c1', 'totalCostCents': 4500}, 201),
  'POST /v1/expenses': (_) => _json({'id': 'd1'}, 201),
};

Future<List<http.Request>> _abrirApp(
  WidgetTester tester,
  Map<String, Rota> rotas, {
  Reconhecedor? microfone,
}) async {
  tester.view.physicalSize = const Size(800, 2000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  final recebidas = <http.Request>[];
  final client = MockClient((req) async {
    recebidas.add(req);
    final rota = rotas['${req.method} ${req.url.path}'];
    return rota != null ? rota(req) : _json({'message': 'sem rota'}, 404);
  });

  await tester.pumpWidget(
    MaterialApp(
      theme: buildAppTheme(),
      home: AppShell(
        user: UserAccount(
          id: 'u1',
          email: 'dono@loja.com',
          name: 'Ana Souza',
          cnpj: null,
          role: 'OWNER',
          storeId: 'loja-1',
          storeName: 'Mercadinho da Ana',
        ),
        api: ApiClient(httpClient: client, baseUrl: 'http://api.test'),
        reconhecedor: microfone ?? _MicrofoneFalso(),
      ),
    ),
  );
  await tester.pumpAndSettle();
  return recebidas;
}

Future<void> _digitarEContinuar(WidgetTester tester, String frase) async {
  await tester.tap(find.text('Toque e fale'));
  await tester.pumpAndSettle();
  expect(find.text('Microfone indisponível. Digite abaixo.'), findsOneWidget);
  await tester.enterText(find.byType(TextField), frase);
  await tester.pump();
  await tester.tap(find.text('Continuar'));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('venda falada monta o carrinho e já escolhe dinheiro com troco', (
    tester,
  ) async {
    const frase = 'vendi 2 refri, o cliente deu 20 no dinheiro';
    final recebidas = await _abrirApp(
      tester,
      _rotasBase(
        (_) => _json(
          _interpretacao(
            'VENDA',
            frase,
            itens: [
              {
                'productId': 'p1',
                'productName': 'Refrigerante',
                'stockUnit': 'UNIT',
                'quantity': 2,
                'unitPriceCents': 600,
              },
            ],
            formaId: 'm1',
            valorPago: 2000,
          ),
        ),
      ),
      microfone: _MicrofoneFalso(frase),
    );

    // Abrir a folha já começa a ouvir; a frase final segue sozinha para a API.
    await tester.tap(find.text('Toque e fale'));
    await tester.pumpAndSettle();

    expect(find.text('Cobrar'), findsOneWidget);
    expect(find.text('R\$ 8,00'), findsOneWidget, reason: 'troco de 20 - 12');

    await tester.tap(find.text('Confirmar venda'));
    await tester.pumpAndSettle();

    final venda = _corpo(recebidas.lastWhere((r) => r.url.path == '/v1/sales'));
    expect(venda['source'], 'VOICE');
    expect(venda['originalInput'], frase);
    expect((venda['payments'] as List).single, {
      'paymentMethodId': 'm1',
      'amountCents': 2000,
      'changeCents': 800,
    });
    final interpretar = _corpo(
      recebidas.firstWhere((r) => r.url.path == '/v1/voice/interpret'),
    );
    expect(interpretar['text'], frase);
  });

  testWidgets('despesa digitada vai para a confirmação e grava como TEXT', (
    tester,
  ) async {
    const frase = 'paguei 180 de luz';
    final recebidas = await _abrirApp(
      tester,
      _rotasBase(
        (_) => _json(
          _interpretacao(
            'DESPESA',
            frase,
            despesa: {
              'description': 'Conta de luz',
              'amountCents': 18000,
              'category': 'ELECTRICITY',
            },
          ),
        ),
      ),
    );

    await _digitarEContinuar(tester, frase);

    expect(find.text('Despesa'), findsOneWidget);
    expect(find.widgetWithText(ChoiceChip, 'Luz'), findsOneWidget);
    await tester.tap(find.text('Registrar despesa'));
    await tester.pumpAndSettle();

    final despesa = _corpo(
      recebidas.lastWhere((r) => r.url.path == '/v1/expenses'),
    );
    expect(despesa['description'], 'Conta de luz');
    expect(despesa['amountCents'], 18000);
    expect(despesa['category'], 'ELECTRICITY');
    expect(despesa['source'], 'TEXT');
    expect(despesa['originalInput'], frase);
    expect(find.text('Despesa registrada.'), findsOneWidget);
  });

  testWidgets('compra mostra o que não achou e grava o custo falado', (
    tester,
  ) async {
    const frase = 'chegou 10 refri a 4,50 e 3 pastel';
    final recebidas = await _abrirApp(
      tester,
      _rotasBase(
        (_) => _json(
          _interpretacao(
            'COMPRA',
            frase,
            itens: [
              {
                'productId': 'p1',
                'productName': 'Refrigerante',
                'stockUnit': 'UNIT',
                'quantity': 10,
                'unitPriceCents': 450,
              },
            ],
            naoEncontrados: ['pastel'],
          ),
        ),
      ),
    );

    await _digitarEContinuar(tester, frase);

    expect(find.text('Entrada de estoque'), findsOneWidget);
    expect(
      find.textContaining('Não achei no cadastro: pastel'),
      findsOneWidget,
    );
    await tester.tap(find.text('Registrar entrada'));
    await tester.pumpAndSettle();

    final compra = _corpo(
      recebidas.lastWhere((r) => r.url.path == '/v1/purchases'),
    );
    expect(compra['items'], [
      {'productId': 'p1', 'quantity': 10.0, 'unitCostCents': 450},
    ]);
    expect(compra['originalInput'], frase);
    expect(
      find.text('Entrada de R\$ 45,00 registrada. Estoque atualizado.'),
      findsOneWidget,
    );
  });

  testWidgets('quando não entende, a folha continua aberta com o aviso', (
    tester,
  ) async {
    await _abrirApp(
      tester,
      _rotasBase(
        (_) => _json(
          _interpretacao(
            'NAO_ENTENDI',
            'bom dia',
            mensagem: 'Não entendi. Pode repetir?',
          ),
        ),
      ),
    );

    await _digitarEContinuar(tester, 'bom dia');

    expect(find.text('Não entendi. Pode repetir?'), findsOneWidget);
    expect(find.text('O que aconteceu?'), findsOneWidget);
  });

  testWidgets('servidor sem a chave do Claude mostra o motivo', (tester) async {
    await _abrirApp(
      tester,
      _rotasBase(
        (_) => _json({
          'message': 'Lançamento por voz não configurado no servidor.',
        }, 503),
      ),
    );

    await _digitarEContinuar(tester, 'vendi um refri');

    expect(
      find.text('Lançamento por voz não configurado no servidor.'),
      findsOneWidget,
    );
  });
}
