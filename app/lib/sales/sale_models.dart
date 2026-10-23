import 'package:flutter/foundation.dart';

import '../core/uuid.dart';
import '../products/product_models.dart';
import '../voice/voice_models.dart';

enum PaymentType {
  cash('CASH'),
  pix('PIX'),
  debitCard('DEBIT_CARD'),
  creditCard('CREDIT_CARD'),
  transfer('TRANSFER'),
  other('OTHER');

  const PaymentType(this.api);

  final String api;

  static PaymentType fromApi(String valor) =>
      values.firstWhere((t) => t.api == valor, orElse: () => other);
}

class PaymentMethod {
  const PaymentMethod({
    required this.id,
    required this.nome,
    required this.tipo,
    required this.aceitaTroco,
  });

  factory PaymentMethod.fromJson(Map<String, dynamic> json) {
    return PaymentMethod(
      id: json['id'] as String,
      nome: json['name'] as String,
      tipo: PaymentType.fromApi(json['type'] as String),
      aceitaTroco: json['acceptsChange'] as bool? ?? false,
    );
  }

  final String id;
  final String nome;
  final PaymentType tipo;
  final bool aceitaTroco;
}

class CartItem {
  const CartItem(this.produto, this.quantidade);

  final Product produto;
  final double quantidade;

  int get totalCentavos => (produto.precoVendaCentavos * quantidade).round();
}

/// Itens da venda em andamento. Fica vivo enquanto a pessoa troca de aba.
class Cart extends ChangeNotifier {
  final _itens = <String, CartItem>{};
  String _chave = gerarUuid();

  /// Frase que montou este carrinho, quando a venda veio por voz ou texto.
  /// Vai junto na venda (source/originalInput) e some ao limpar.
  Interpretacao? origem;

  List<CartItem> get itens => _itens.values.toList(growable: false);

  bool get vazio => _itens.isEmpty;

  int get totalCentavos =>
      _itens.values.fold(0, (soma, item) => soma + item.totalCentavos);

  double quantidadeDe(String productId) => _itens[productId]?.quantidade ?? 0;

  /// Chave de idempotência da venda: reenviar a mesma venda (ex.: a resposta se
  /// perdeu na rede) não duplica nada. Muda sempre que o carrinho muda, para uma
  /// venda diferente nunca reaproveitar a chave de outra.
  String get chaveIdempotencia => _chave;

  void adicionar(Product produto, [double quantidade = 1]) {
    definirQuantidade(produto, quantidadeDe(produto.id) + quantidade);
  }

  void definirQuantidade(Product produto, double quantidade) {
    if (quantidade <= 0) {
      _itens.remove(produto.id);
    } else {
      _itens[produto.id] = CartItem(produto, quantidade);
    }
    _mudou();
  }

  void remover(String productId) {
    if (_itens.remove(productId) != null) _mudou();
  }

  void limpar() {
    _itens.clear();
    origem = null;
    _mudou();
  }

  void _mudou() {
    _chave = gerarUuid();
    notifyListeners();
  }
}

/// Resultado do /v1/sales/preview: totais calculados pelo servidor e avisos.
class SalePreview {
  const SalePreview({
    required this.totalCentavos,
    required this.lucroCentavos,
    required this.margemPercentual,
    required this.avisos,
  });

  factory SalePreview.fromJson(Map<String, dynamic> json) {
    return SalePreview(
      totalCentavos: json['totalAmountCents'] as int,
      lucroCentavos: json['grossProfitCents'] as int,
      margemPercentual: (json['marginPercent'] as num?)?.toDouble() ?? 0,
      avisos: (json['warnings'] as List? ?? const []).cast<String>(),
    );
  }

  final int totalCentavos;
  final int lucroCentavos;
  final double margemPercentual;
  final List<String> avisos;
}

class SaleResult {
  const SaleResult({required this.id, required this.totalCentavos});

  factory SaleResult.fromJson(Map<String, dynamic> json) {
    return SaleResult(
      id: json['id'] as String,
      totalCentavos: json['totalAmountCents'] as int,
    );
  }

  final String id;
  final int totalCentavos;
}
