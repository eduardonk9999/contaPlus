import 'dart:convert';

import 'package:contaplus/core/api_client.dart';
import 'package:contaplus/core/uuid.dart';
import 'package:contaplus/products/product_models.dart';
import 'package:contaplus/products/product_service.dart';
import 'package:contaplus/sales/sale_models.dart';
import 'package:contaplus/sales/sale_page.dart';
import 'package:contaplus/sales/sale_service.dart';
import 'package:contaplus/theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

typedef Rota = http.Response Function(http.Request req);

final _formatoUuid = RegExp(r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$');

http.Response _json(Object? body, [int status = 200]) =>
    http.Response(jsonEncode(body), status, headers: {'content-type': 'application/json'});

const _precos = {'p1': 600, 'p2': 3990};

Map<String, dynamic> _produtoJson(String id, String nome, String unidade, num estoque) => {
      'id': id,
      'storeId': 'loja-1',
      'categoryId': null,
      'name': nome,
      'type': 'SELLABLE',
      'costPriceCents': 300,
      'salePriceCents': _precos[id],
      'stockQuantity': estoque,
      'stockUnit': unidade,
      'minStockQuantity': 0,
      'active': true,
    };

Product _produto(String id, {StockUnit unidade = StockUnit.unit}) => Product(
      id: id,
      nome: id,
      categoriaId: null,
      tipo: ProductType.sellable,
      custoCentavos: 300,
      precoVendaCentavos: _precos[id]!,
      estoque: 10,
      estoqueMinimo: 0,
      unidade: unidade,
      ativo: true,
    );

/// Total como a API calcularia, a partir dos itens enviados no preview.
int _totalDe(http.Request req) {
  final itens = (jsonDecode(req.body) as Map)['items'] as List;
  return itens.fold<int>(
    0,
    (soma, i) => soma + ((_precos[i['productId']]! * (i['quantity'] as num)).round()),
  );
}

Map<String, Rota> _rotasPadrao({List<String> avisos = const []}) => {
      'GET /v1/products': (_) => _json({
            'content': [
              _produtoJson('p2', 'Pão de queijo', 'KG', 2.5),
              _produtoJson('p1', 'Refrigerante', 'UNIT', 30),
            ],
            'page': 0,
            'size': 50,
            'totalElements': 2,
            'totalPages': 1,
            'first': true,
            'last': true,
          }),
      'POST /v1/sales/preview': (req) {
        final total = _totalDe(req);
        return _json({
          'items': [],
          'totalAmountCents': total,
          'totalCostCents': 0,
          'grossProfitCents': total ~/ 2,
          'marginPercent': 50,
          'warnings': avisos,
        });
      },
      'GET /v1/payment-methods': (_) => _json([
            {'id': 'm2', 'name': 'PIX', 'type': 'PIX', 'acceptsChange': false, 'active': true},
            {'id': 'm1', 'name': 'Dinheiro', 'type': 'CASH', 'acceptsChange': true, 'active': true},
          ]),
    };

Future<List<http.Request>> _abrir(WidgetTester tester, Map<String, Rota> rotas) async {
  tester.view.physicalSize = const Size(800, 2000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  final recebidas = <http.Request>[];
  final client = MockClient((req) async {
    recebidas.add(req);
    final rota = rotas['${req.method} ${req.url.path}'];
    return rota != null ? rota(req) : _json({'message': 'sem rota'}, 404);
  });
  final api = ApiClient(httpClient: client, baseUrl: 'http://api.test');

  await tester.pumpWidget(MaterialApp(
    theme: buildAppTheme(),
    home: SalePage(
      produtos: ProductService(api),
      vendas: SaleService(api),
      cart: Cart(),
      storeId: 'loja-1',
    ),
  ));
  await tester.pumpAndSettle();
  return recebidas;
}

Map<String, dynamic> _corpo(http.Request req) => jsonDecode(req.body) as Map<String, dynamic>;

Future<void> _irParaCobranca(WidgetTester tester) async {
  await tester.tap(find.text('Cobrar'));
  await tester.pumpAndSettle();
}

void main() {
  group('carrinho', () {
    test('soma os itens e remove ao zerar', () {
      final cart = Cart()
        ..adicionar(_produto('p1'))
        ..adicionar(_produto('p1'))
        ..definirQuantidade(_produto('p2', unidade: StockUnit.kg), 0.5);

      expect(cart.totalCentavos, 600 * 2 + 1995);

      cart.definirQuantidade(_produto('p1'), 0);
      expect(cart.itens.map((i) => i.produto.id), ['p2']);
    });

    test('a chave de idempotência só muda quando o carrinho muda', () {
      final cart = Cart();
      final inicial = cart.chaveIdempotencia;
      expect(cart.chaveIdempotencia, inicial);

      cart.adicionar(_produto('p1'));
      expect(cart.chaveIdempotencia, isNot(inicial));
      expect(cart.chaveIdempotencia, matches(_formatoUuid));
    });
  });

  test('gera UUID v4 válido e diferente a cada chamada', () {
    final a = gerarUuid();
    expect(a, matches(_formatoUuid));
    expect(gerarUuid(), isNot(a));
  });

  testWidgets('lista só produtos à venda e monta o carrinho', (tester) async {
    final recebidas = await _abrir(tester, _rotasPadrao());

    expect(recebidas.first.url.queryParameters['type'], 'SELLABLE');

    await tester.tap(find.text('Refrigerante'));
    await tester.tap(find.text('Refrigerante'));
    await tester.pump();

    expect(find.text('2'), findsOneWidget);
    expect(find.text('1 item'), findsOneWidget);
    expect(find.text('R\$ 12,00'), findsOneWidget);
  });

  testWidgets('item vendido por peso pergunta a quantidade', (tester) async {
    await _abrir(tester, _rotasPadrao());

    await tester.tap(find.text('Pão de queijo'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).last, '0,5');
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();

    expect(find.text('0,5 kg'), findsOneWidget);
    expect(find.text('R\$ 19,95'), findsOneWidget);
  });

  testWidgets('cobra em dinheiro com troco e envia venda e pagamento juntos', (tester) async {
    Map<String, dynamic>? enviado;
    final rotas = _rotasPadrao()
      ..['POST /v1/sales'] = (req) {
        enviado = _corpo(req);
        return _json({'id': 's1', 'totalAmountCents': 1200}, 201);
      };
    await _abrir(tester, rotas);

    await tester.tap(find.text('Refrigerante'));
    await tester.tap(find.text('Refrigerante'));
    await tester.pump();
    await _irParaCobranca(tester);

    expect(find.text('Lucro estimado R\$ 6,00 · margem 50%'), findsOneWidget);

    await tester.tap(find.text('Dinheiro'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Valor recebido'), '2000');
    await tester.pump();
    expect(find.text('Troco'), findsOneWidget);
    expect(find.text('R\$ 8,00'), findsOneWidget);

    await tester.tap(find.text('Confirmar venda'));
    await tester.pumpAndSettle();

    expect(enviado!['idempotencyKey'], matches(_formatoUuid));
    expect(enviado!['items'], [
      {'productId': 'p1', 'quantity': 2},
    ]);
    expect(enviado!['payments'], [
      {'paymentMethodId': 'm1', 'amountCents': 2000, 'changeCents': 800},
    ]);
    expect(find.text('Venda registrada'), findsOneWidget);
    expect(find.text('Troco a devolver'), findsOneWidget);

    await tester.tap(find.text('Nova venda'));
    await tester.pumpAndSettle();
    expect(find.text('Cobrar'), findsNothing);
  });

  testWidgets('PIX cobra o valor exato, sem troco', (tester) async {
    Map<String, dynamic>? enviado;
    final rotas = _rotasPadrao()
      ..['POST /v1/sales'] = (req) {
        enviado = _corpo(req);
        return _json({'id': 's1', 'totalAmountCents': 600}, 201);
      };
    await _abrir(tester, rotas);

    await tester.tap(find.text('Refrigerante'));
    await tester.pump();
    await _irParaCobranca(tester);
    await tester.tap(find.text('PIX'));
    await tester.pumpAndSettle();

    expect(find.widgetWithText(TextField, 'Valor recebido'), findsNothing);

    await tester.tap(find.text('Confirmar venda'));
    await tester.pumpAndSettle();

    expect(enviado!['payments'], [
      {'paymentMethodId': 'm2', 'amountCents': 600, 'changeCents': 0},
    ]);
  });

  testWidgets('dinheiro a menos bloqueia a confirmação', (tester) async {
    await _abrir(tester, _rotasPadrao());

    await tester.tap(find.text('Refrigerante'));
    await tester.pump();
    await _irParaCobranca(tester);
    await tester.tap(find.text('Dinheiro'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Valor recebido'), '500');
    await tester.pump();

    expect(find.text('Faltam'), findsOneWidget);
    expect(find.text('R\$ 1,00'), findsOneWidget);
    final botao = tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Confirmar venda'));
    expect(botao.onPressed, isNull);
  });

  testWidgets('mostra os avisos de estoque calculados pela API', (tester) async {
    await _abrir(tester, _rotasPadrao(avisos: ["'Refrigerante' vai ficar com estoque negativo (-1)"]));

    await tester.tap(find.text('Refrigerante'));
    await tester.pump();
    await _irParaCobranca(tester);

    expect(find.text("'Refrigerante' vai ficar com estoque negativo (-1)"), findsOneWidget);
  });

  testWidgets('sem resposta do servidor, tentar de novo reenvia a mesma venda', (tester) async {
    final chaves = <String>[];
    var tentativas = 0;
    final rotas = _rotasPadrao()
      ..['POST /v1/sales'] = (req) {
        chaves.add(_corpo(req)['idempotencyKey'] as String);
        if (++tentativas == 1) throw http.ClientException('Connection reset');
        return _json({'id': 's1', 'totalAmountCents': 600}, 201);
      };
    await _abrir(tester, rotas);

    await tester.tap(find.text('Refrigerante'));
    await tester.pump();
    await _irParaCobranca(tester);
    await tester.tap(find.text('PIX'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Confirmar venda'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Sem resposta do servidor'), findsOneWidget);

    await tester.tap(find.text('Confirmar venda'));
    await tester.pumpAndSettle();

    expect(find.text('Venda registrada'), findsOneWidget);
    expect(chaves, hasLength(2));
    expect(chaves[1], chaves[0]);

    await tester.pump(const Duration(seconds: 5));
    await tester.pumpAndSettle();
  });
}
