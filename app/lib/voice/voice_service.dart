import '../core/api_client.dart';
import 'voice_models.dart';

/// /v1/voice/interpret e os registros que saem dele (compra e despesa; venda usa o SaleService).
class VoiceService {
  VoiceService(this._api);

  final ApiClient _api;

  Future<Interpretacao> interpretar(String texto, FonteLancamento fonte) async {
    final json = await _api.post('/v1/voice/interpret', {'text': texto});
    return Interpretacao.fromJson(json as Map<String, dynamic>, fonte);
  }

  /// Entrada de estoque: a API soma as quantidades e atualiza o custo dos produtos.
  Future<int> registrarCompra({
    required String storeId,
    required String chaveIdempotencia,
    required List<ItemInterpretado> itens,
    required Interpretacao origem,
  }) async {
    final json = await _api.post('/v1/purchases', {
      'storeId': storeId,
      'idempotencyKey': chaveIdempotencia,
      'items': [
        for (final item in itens)
          {
            'productId': item.productId,
            'quantity': item.quantidade,
            if (item.precoUnitarioCentavos > 0)
              'unitCostCents': item.precoUnitarioCentavos,
          },
      ],
      'source': origem.fonte.api,
      'originalInput': origem.frase,
    }) as Map<String, dynamic>;
    return json['totalCostCents'] as int? ?? 0;
  }

  Future<void> registrarDespesa({
    required String storeId,
    required String chaveIdempotencia,
    required String descricao,
    required int valorCentavos,
    required ExpenseCategory categoria,
    required Interpretacao origem,
  }) async {
    await _api.post('/v1/expenses', {
      'storeId': storeId,
      'idempotencyKey': chaveIdempotencia,
      'description': descricao,
      'amountCents': valorCentavos,
      'category': categoria.api,
      'source': origem.fonte.api,
      'originalInput': origem.frase,
    });
  }
}
