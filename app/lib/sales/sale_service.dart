import '../core/api_client.dart';
import '../voice/voice_models.dart';
import 'sale_models.dart';

/// Endpoints /v1/sales e /v1/payment-methods da API Conta+.
class SaleService {
  SaleService(this._api);

  final ApiClient _api;

  Future<List<PaymentMethod>> formasPagamento() async {
    final json = await _api.get('/v1/payment-methods') as List;
    final formas = json
        .map((item) => PaymentMethod.fromJson(item as Map<String, dynamic>))
        .toList();
    // Ordem de uso no balcão: dinheiro, pix, débito, crédito...
    formas.sort((a, b) => a.tipo.index.compareTo(b.tipo.index));
    return formas;
  }

  /// Totais e avisos calculados pela API, sem gravar nada.
  Future<SalePreview> preview(String storeId, List<CartItem> itens) async {
    final json = await _api.post('/v1/sales/preview', {
      'storeId': storeId,
      'items': _itens(itens),
    });
    return SalePreview.fromJson(json as Map<String, dynamic>);
  }

  /// Venda e pagamento são gravados juntos: se o pagamento não cobre o total,
  /// a API não grava nada.
  Future<SaleResult> confirmar({
    required String storeId,
    required String chaveIdempotencia,
    required List<CartItem> itens,
    required PaymentMethod forma,
    required int valorPagoCentavos,
    required int trocoCentavos,
    Interpretacao? origem,
  }) async {
    final json = await _api.post('/v1/sales', {
      'storeId': storeId,
      'idempotencyKey': chaveIdempotencia,
      'items': _itens(itens),
      if (origem != null) ...{
        'source': origem.fonte.api,
        'originalInput': origem.frase,
      },
      'payments': [
        {
          'paymentMethodId': forma.id,
          'amountCents': valorPagoCentavos,
          'changeCents': trocoCentavos,
        },
      ],
    });
    return SaleResult.fromJson(json as Map<String, dynamic>);
  }

  List<Map<String, dynamic>> _itens(List<CartItem> itens) => [
    for (final item in itens)
      {'productId': item.produto.id, 'quantity': item.quantidade},
  ];
}
