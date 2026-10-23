import '../products/product_models.dart';

/// Como o lançamento chegou: falado no microfone ou digitado.
enum FonteLancamento {
  voz('VOICE'),
  texto('TEXT');

  const FonteLancamento(this.api);

  final String api;
}

enum TipoLancamento {
  venda('VENDA'),
  compra('COMPRA'),
  despesa('DESPESA'),
  naoEntendi('NAO_ENTENDI');

  const TipoLancamento(this.api);

  final String api;

  static TipoLancamento fromApi(String valor) =>
      values.firstWhere((t) => t.api == valor, orElse: () => naoEntendi);
}

enum ExpenseCategory {
  rent('RENT', 'Aluguel'),
  electricity('ELECTRICITY', 'Luz'),
  water('WATER', 'Água'),
  internetPhone('INTERNET_PHONE', 'Internet e telefone'),
  payroll('PAYROLL', 'Salários'),
  taxes('TAXES', 'Impostos'),
  maintenance('MAINTENANCE', 'Manutenção'),
  transport('TRANSPORT', 'Transporte'),
  other('OTHER', 'Outros');

  const ExpenseCategory(this.api, this.rotulo);

  final String api;
  final String rotulo;

  static ExpenseCategory fromApi(String? valor) =>
      values.firstWhere((c) => c.api == valor, orElse: () => other);
}

/// Item reconhecido na frase. Em venda o preço é o de venda; em compra, o custo.
class ItemInterpretado {
  const ItemInterpretado({
    required this.productId,
    required this.nome,
    required this.unidade,
    required this.quantidade,
    required this.precoUnitarioCentavos,
  });

  factory ItemInterpretado.fromJson(Map<String, dynamic> json) {
    return ItemInterpretado(
      productId: json['productId'] as String,
      nome: json['productName'] as String,
      unidade: StockUnit.fromApi(json['stockUnit'] as String),
      quantidade: (json['quantity'] as num).toDouble(),
      precoUnitarioCentavos: json['unitPriceCents'] as int? ?? 0,
    );
  }

  final String productId;
  final String nome;
  final StockUnit unidade;
  final double quantidade;
  final int precoUnitarioCentavos;

  int get totalCentavos => (precoUnitarioCentavos * quantidade).round();

  ItemInterpretado copiar({double? quantidade, int? precoUnitarioCentavos}) =>
      ItemInterpretado(
        productId: productId,
        nome: nome,
        unidade: unidade,
        quantidade: quantidade ?? this.quantidade,
        precoUnitarioCentavos:
            precoUnitarioCentavos ?? this.precoUnitarioCentavos,
      );
}

class DespesaInterpretada {
  const DespesaInterpretada({
    required this.descricao,
    required this.valorCentavos,
    required this.categoria,
  });

  factory DespesaInterpretada.fromJson(Map<String, dynamic> json) {
    return DespesaInterpretada(
      descricao: json['description'] as String,
      valorCentavos: json['amountCents'] as int,
      categoria: ExpenseCategory.fromApi(json['category'] as String?),
    );
  }

  final String descricao;
  final int valorCentavos;
  final ExpenseCategory categoria;
}

/// Resposta do /v1/voice/interpret. Nada foi gravado ainda: o lojista confirma antes.
class Interpretacao {
  const Interpretacao({
    required this.tipo,
    required this.frase,
    required this.fonte,
    required this.itens,
    required this.naoEncontrados,
    required this.formaPagamentoId,
    required this.valorPagoCentavos,
    required this.despesa,
    required this.mensagem,
  });

  factory Interpretacao.fromJson(
    Map<String, dynamic> json,
    FonteLancamento fonte,
  ) {
    final despesa = json['despesa'] as Map<String, dynamic>?;
    return Interpretacao(
      tipo: TipoLancamento.fromApi(json['tipo'] as String),
      frase: json['frase'] as String,
      fonte: fonte,
      itens: (json['itens'] as List? ?? const [])
          .map(
            (item) => ItemInterpretado.fromJson(item as Map<String, dynamic>),
          )
          .toList(),
      naoEncontrados: (json['naoEncontrados'] as List? ?? const [])
          .cast<String>(),
      formaPagamentoId: json['paymentMethodId'] as String?,
      valorPagoCentavos: json['valorPagoCentavos'] as int?,
      despesa: despesa == null ? null : DespesaInterpretada.fromJson(despesa),
      mensagem: json['mensagem'] as String?,
    );
  }

  final TipoLancamento tipo;
  final String frase;
  final FonteLancamento fonte;
  final List<ItemInterpretado> itens;
  final List<String> naoEncontrados;
  final String? formaPagamentoId;
  final int? valorPagoCentavos;
  final DespesaInterpretada? despesa;
  final String? mensagem;

  /// Dá pra seguir para a confirmação? Senão a folha mostra [mensagem] e pede de novo.
  bool get acionavel => switch (tipo) {
    TipoLancamento.venda || TipoLancamento.compra => itens.isNotEmpty,
    TipoLancamento.despesa => despesa != null,
    TipoLancamento.naoEntendi => false,
  };
}
