enum ProductType {
  sellable('SELLABLE', 'Para venda'),
  supply('SUPPLY', 'Insumo');

  const ProductType(this.api, this.rotulo);

  final String api;
  final String rotulo;

  static ProductType fromApi(String valor) =>
      values.firstWhere((t) => t.api == valor, orElse: () => sellable);
}

enum StockUnit {
  unit('UNIT', 'un', 'Unidade'),
  kg('KG', 'kg', 'Quilo'),
  g('G', 'g', 'Grama'),
  l('L', 'L', 'Litro'),
  ml('ML', 'mL', 'Mililitro'),
  m('M', 'm', 'Metro'),
  cm('CM', 'cm', 'Centímetro');

  const StockUnit(this.api, this.sigla, this.nome);

  final String api;
  final String sigla;
  final String nome;

  static StockUnit fromApi(String valor) =>
      values.firstWhere((u) => u.api == valor, orElse: () => unit);
}

/// Espelha o ProductResponse da API. Dinheiro em centavos.
class Product {
  const Product({
    required this.id,
    required this.nome,
    required this.categoriaId,
    required this.tipo,
    required this.custoCentavos,
    required this.precoVendaCentavos,
    required this.estoque,
    required this.estoqueMinimo,
    required this.unidade,
    required this.ativo,
  });

  factory Product.fromJson(Map<String, dynamic> json) {
    return Product(
      id: json['id'] as String,
      nome: json['name'] as String,
      categoriaId: json['categoryId'] as String?,
      tipo: ProductType.fromApi(json['type'] as String),
      custoCentavos: json['costPriceCents'] as int,
      precoVendaCentavos: json['salePriceCents'] as int,
      estoque: (json['stockQuantity'] as num?)?.toDouble() ?? 0,
      estoqueMinimo: (json['minStockQuantity'] as num?)?.toDouble() ?? 0,
      unidade: StockUnit.fromApi(json['stockUnit'] as String),
      ativo: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String nome;
  final String? categoriaId;
  final ProductType tipo;
  final int custoCentavos;
  final int precoVendaCentavos;
  final double estoque;
  final double estoqueMinimo;
  final StockUnit unidade;
  final bool ativo;

  bool get semEstoque => estoque <= 0;

  bool get estoqueBaixo => !semEstoque && estoqueMinimo > 0 && estoque <= estoqueMinimo;
}

/// Dados que o formulário envia para criar ou atualizar um produto.
class ProductInput {
  const ProductInput({
    required this.nome,
    required this.tipo,
    required this.custoCentavos,
    required this.precoVendaCentavos,
    required this.unidade,
    required this.categoriaId,
    required this.estoqueMinimo,
  });

  final String nome;
  final ProductType tipo;
  final int custoCentavos;
  final int precoVendaCentavos;
  final StockUnit unidade;
  final String? categoriaId;
  final double estoqueMinimo;
}

class Category {
  const Category({required this.id, required this.nome});

  factory Category.fromJson(Map<String, dynamic> json) {
    return Category(id: json['id'] as String, nome: json['name'] as String);
  }

  final String id;
  final String nome;
}

/// Uma página de resultados do PageResponse da API.
class PageResult<T> {
  const PageResult({required this.itens, required this.pagina, required this.ultima});

  final List<T> itens;
  final int pagina;
  final bool ultima;
}
