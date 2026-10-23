import '../core/api_client.dart';
import 'product_models.dart';

/// Endpoints /v1/products e /v1/categories da API Conta+.
class ProductService {
  ProductService(this._api);

  final ApiClient _api;

  Future<PageResult<Product>> listar({
    String busca = '',
    int pagina = 0,
    int tamanho = 20,
    ProductType? tipo,
  }) async {
    final query = Uri(
      queryParameters: {
        'page': '$pagina',
        'size': '$tamanho',
        'sortBy': 'name',
        'sortDir': 'asc',
        if (busca.isNotEmpty) 'search': busca,
        if (tipo != null) 'type': tipo.api,
      },
    ).query;
    final json = await _api.get('/v1/products?$query') as Map<String, dynamic>;
    return PageResult(
      itens: (json['content'] as List)
          .map((item) => Product.fromJson(item as Map<String, dynamic>))
          .toList(),
      pagina: json['page'] as int,
      ultima: json['last'] as bool,
    );
  }

  Future<Product> buscar(String id) async {
    final json = await _api.get('/v1/products/$id');
    return Product.fromJson(json as Map<String, dynamic>);
  }

  Future<Product> criar(String storeId, ProductInput dados) async {
    final json = await _api.post('/v1/products', {
      'storeId': storeId,
      'name': dados.nome,
      'type': dados.tipo.api,
      'costPriceCents': dados.custoCentavos,
      'salePriceCents': dados.precoVendaCentavos,
      'stockUnit': dados.unidade.api,
      'categoryId': dados.categoriaId,
      'minStockQuantity': dados.estoqueMinimo,
    });
    return Product.fromJson(json as Map<String, dynamic>);
  }

  /// A API sobrescreve categoria e estoque mínimo: mande sempre os valores atuais,
  /// senão a categoria some e o mínimo vira zero.
  Future<Product> atualizar(String id, ProductInput dados) async {
    final json = await _api.put('/v1/products/$id', {
      'name': dados.nome,
      'type': dados.tipo.api,
      'costPriceCents': dados.custoCentavos,
      'salePriceCents': dados.precoVendaCentavos,
      'stockUnit': dados.unidade.api,
      'minStockQuantity': dados.estoqueMinimo,
      'categoryId': dados.categoriaId,
    });
    return Product.fromJson(json as Map<String, dynamic>);
  }

  Future<void> desativar(String id) => _api.delete('/v1/products/$id');

  Future<List<Category>> categorias() async {
    final json = await _api.get('/v1/categories') as List;
    return json
        .map((item) => Category.fromJson(item as Map<String, dynamic>))
        .toList();
  }
}
