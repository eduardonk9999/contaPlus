import 'dart:convert';

import 'package:contaplus/core/api_client.dart';
import 'package:contaplus/products/product_service.dart';
import 'package:contaplus/products/products_page.dart';
import 'package:contaplus/theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

typedef Rota = http.Response Function(http.Request req);

http.Response _json(Object? body, [int status = 200]) =>
    http.Response(jsonEncode(body), status, headers: {'content-type': 'application/json'});

Map<String, dynamic> _produto({
  String id = 'p1',
  String nome = 'Refrigerante',
  String? categoria,
  int preco = 600,
  int custo = 300,
  num estoque = 10,
  num minimo = 0,
  String unidade = 'UNIT',
  String tipo = 'SELLABLE',
}) =>
    {
      'id': id,
      'storeId': 'loja-1',
      'categoryId': categoria,
      'name': nome,
      'type': tipo,
      'costPriceCents': custo,
      'salePriceCents': preco,
      'stockQuantity': estoque,
      'stockUnit': unidade,
      'minStockQuantity': minimo,
      'active': true,
    };

Map<String, dynamic> _pagina(List<Map<String, dynamic>> itens) => {
      'content': itens,
      'page': 0,
      'size': 20,
      'totalElements': itens.length,
      'totalPages': 1,
      'first': true,
      'last': true,
    };

/// Abre a tela de produtos com a API simulada; devolve as requisições recebidas.
Future<List<http.Request>> _abrir(WidgetTester tester, Map<String, Rota> rotas) async {
  // Tela alta o bastante para o formulário inteiro caber sem rolar.
  tester.view.physicalSize = const Size(800, 2000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  final recebidas = <http.Request>[];
  final client = MockClient((req) async {
    recebidas.add(req);
    final rota = rotas['${req.method} ${req.url.path}'];
    return rota != null ? rota(req) : _json({'message': 'sem rota'}, 404);
  });
  final service = ProductService(ApiClient(httpClient: client, baseUrl: 'http://api.test'));

  await tester.pumpWidget(MaterialApp(
    theme: buildAppTheme(),
    home: ProductsPage(service: service, storeId: 'loja-1'),
  ));
  await tester.pumpAndSettle();
  return recebidas;
}

Finder _campo(String rotulo) => find.widgetWithText(TextFormField, rotulo);

/// O SnackBar deixa um timer pendente; deixa expirar antes do fim do teste.
Future<void> _esperarSnackBar(WidgetTester tester) async {
  await tester.pump(const Duration(seconds: 5));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('lista produtos com preço, estoque e categoria', (tester) async {
    await _abrir(tester, {
      'GET /v1/categories': (_) => _json([
            {'id': 'c1', 'name': 'Bebidas'},
          ]),
      'GET /v1/products': (_) => _json(_pagina([
            _produto(nome: 'Água', estoque: 2, minimo: 5, preco: 300, id: 'p2'),
            _produto(nome: 'Farinha', tipo: 'SUPPLY', estoque: 0, unidade: 'KG', id: 'p3'),
            _produto(nome: 'Refrigerante', categoria: 'c1'),
          ])),
    });

    expect(find.text('Refrigerante'), findsOneWidget);
    expect(find.text('R\$ 6,00'), findsWidgets);
    expect(find.text('Bebidas'), findsOneWidget);
    expect(find.text('Estoque: 10 un'), findsOneWidget);
    expect(find.text('Estoque baixo · 2 un'), findsOneWidget);
    expect(find.text('Insumo'), findsOneWidget);
    expect(find.text('Sem estoque'), findsOneWidget);
  });

  testWidgets('sem produtos, convida a cadastrar', (tester) async {
    await _abrir(tester, {
      'GET /v1/categories': (_) => _json([]),
      'GET /v1/products': (_) => _json(_pagina([])),
    });

    expect(find.text('Nenhum produto cadastrado'), findsOneWidget);
    expect(find.text('Cadastrar produto'), findsOneWidget);
  });

  testWidgets('busca só consulta a API depois que a pessoa para de digitar', (tester) async {
    final recebidas = await _abrir(tester, {
      'GET /v1/categories': (_) => _json([]),
      'GET /v1/products': (req) => _json(_pagina([
            if (req.url.queryParameters['search'] == 'refri') _produto(nome: 'Refrigerante'),
          ])),
    });
    final buscasAntes = recebidas.where((r) => r.url.path == '/v1/products').length;

    await tester.enterText(find.byType(TextField), 'refri');
    await tester.pump(const Duration(milliseconds: 300));
    expect(recebidas.where((r) => r.url.path == '/v1/products').length, buscasAntes);

    await tester.pump(const Duration(milliseconds: 150));
    await tester.pumpAndSettle();
    final ultima = recebidas.lastWhere((r) => r.url.path == '/v1/products');
    expect(ultima.url.queryParameters['search'], 'refri');
    expect(find.text('Refrigerante'), findsOneWidget);
  });

  testWidgets('cadastra produto enviando valores em centavos', (tester) async {
    final produtos = <Map<String, dynamic>>[];
    Map<String, dynamic>? enviado;
    await _abrir(tester, {
      'GET /v1/categories': (_) => _json([]),
      'GET /v1/products': (_) => _json(_pagina(produtos)),
      'POST /v1/products': (req) {
        enviado = jsonDecode(req.body) as Map<String, dynamic>;
        final criado = _produto(nome: 'Pão de queijo', preco: 1250, custo: 800, estoque: 0, minimo: 5);
        produtos.add(criado);
        return _json(criado, 201);
      },
    });

    await tester.tap(find.text('Novo produto'));
    await tester.pumpAndSettle();

    await tester.enterText(_campo('Nome'), 'Pão de queijo');
    await tester.enterText(_campo('Preço de venda'), '1250');
    await tester.enterText(_campo('Custo'), '800');
    await tester.enterText(_campo('Estoque mínimo (opcional)'), '5');
    await tester.pump();

    expect(find.text('R\$ 12,50'), findsOneWidget);
    expect(find.text('Margem de 36% · lucro de R\$ 4,50 por un'), findsOneWidget);

    await tester.tap(find.text('Cadastrar produto'));
    await tester.pumpAndSettle();

    expect(enviado, {
      'storeId': 'loja-1',
      'name': 'Pão de queijo',
      'type': 'SELLABLE',
      'costPriceCents': 800,
      'salePriceCents': 1250,
      'stockUnit': 'UNIT',
      'categoryId': null,
      'minStockQuantity': 5,
    });
    expect(find.text('Produto cadastrado'), findsOneWidget);
    expect(find.text('Pão de queijo'), findsOneWidget);
    await _esperarSnackBar(tester);
  });

  testWidgets('edição reenvia a categoria e o estoque mínimo atuais', (tester) async {
    Map<String, dynamic>? enviado;
    await _abrir(tester, {
      'GET /v1/categories': (_) => _json([
            {'id': 'c1', 'name': 'Bebidas'},
          ]),
      'GET /v1/products': (_) => _json(_pagina([_produto(categoria: 'c1', minimo: 5)])),
      'PUT /v1/products/p1': (req) {
        enviado = jsonDecode(req.body) as Map<String, dynamic>;
        return _json(_produto(nome: 'Refrigerante lata', categoria: 'c1', minimo: 5));
      },
    });

    await tester.tap(find.text('Refrigerante'));
    await tester.pumpAndSettle();
    expect(find.text('Editar produto'), findsOneWidget);
    expect(find.text('Bebidas'), findsOneWidget);

    await tester.enterText(_campo('Nome'), 'Refrigerante lata');
    await tester.tap(find.text('Salvar alterações'));
    await tester.pumpAndSettle();

    expect(enviado, {
      'name': 'Refrigerante lata',
      'type': 'SELLABLE',
      'costPriceCents': 300,
      'salePriceCents': 600,
      'stockUnit': 'UNIT',
      'minStockQuantity': 5,
      'categoryId': 'c1',
    });
    expect(find.text('Produto atualizado'), findsOneWidget);
    await _esperarSnackBar(tester);
  });

  testWidgets('não envia o formulário sem nome e preço', (tester) async {
    final recebidas = await _abrir(tester, {
      'GET /v1/categories': (_) => _json([]),
      'GET /v1/products': (_) => _json(_pagina([])),
    });

    await tester.tap(find.text('Novo produto'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cadastrar produto'));
    await tester.pumpAndSettle();

    expect(find.text('Informe o nome do produto'), findsOneWidget);
    expect(find.text('Informe o preço'), findsOneWidget);
    expect(recebidas.where((r) => r.method == 'POST'), isEmpty);
  });

  testWidgets('insumo pode ser cadastrado sem preço de venda', (tester) async {
    Map<String, dynamic>? enviado;
    await _abrir(tester, {
      'GET /v1/categories': (_) => _json([]),
      'GET /v1/products': (_) => _json(_pagina([])),
      'POST /v1/products': (req) {
        enviado = jsonDecode(req.body) as Map<String, dynamic>;
        return _json(_produto(nome: 'Farinha', tipo: 'SUPPLY', preco: 0, custo: 450), 201);
      },
    });

    await tester.tap(find.text('Novo produto'));
    await tester.pumpAndSettle();
    await tester.enterText(_campo('Nome'), 'Farinha');
    await tester.tap(find.text('Insumo'));
    await tester.pumpAndSettle();
    await tester.enterText(_campo('Custo'), '450');
    await tester.tap(find.text('Cadastrar produto'));
    await tester.pumpAndSettle();

    expect(enviado?['type'], 'SUPPLY');
    expect(enviado?['salePriceCents'], 0);
    expect(enviado?['costPriceCents'], 450);
    await _esperarSnackBar(tester);
  });

  testWidgets('desativa o produto depois de confirmar', (tester) async {
    final recebidas = await _abrir(tester, {
      'GET /v1/categories': (_) => _json([]),
      'GET /v1/products': (_) => _json(_pagina([_produto()])),
      'DELETE /v1/products/p1': (_) => http.Response('', 204),
    });

    await tester.tap(find.text('Refrigerante'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Desativar produto'));
    await tester.pumpAndSettle();
    expect(find.text('Desativar produto?'), findsOneWidget);

    await tester.tap(find.text('Desativar'));
    await tester.pumpAndSettle();

    expect(recebidas.where((r) => r.method == 'DELETE'), hasLength(1));
    expect(find.text('Produto desativado'), findsOneWidget);
    await _esperarSnackBar(tester);
  });

  testWidgets('mostra o erro da API e continua no formulário', (tester) async {
    await _abrir(tester, {
      'GET /v1/categories': (_) => _json([]),
      'GET /v1/products': (_) => _json(_pagina([])),
      'POST /v1/products': (_) =>
          _json({'status': 400, 'message': "Já existe um produto chamado 'Água' nesta loja"}, 400),
    });

    await tester.tap(find.text('Novo produto'));
    await tester.pumpAndSettle();
    await tester.enterText(_campo('Nome'), 'Água');
    await tester.enterText(_campo('Preço de venda'), '300');
    await tester.tap(find.text('Cadastrar produto'));
    await tester.pumpAndSettle();

    expect(find.text("Já existe um produto chamado 'Água' nesta loja"), findsOneWidget);
    expect(find.text('Novo produto'), findsOneWidget);
    await _esperarSnackBar(tester);
  });
}
