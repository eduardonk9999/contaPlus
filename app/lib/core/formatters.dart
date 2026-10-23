import 'package:flutter/services.dart';
import 'package:intl/intl.dart';

final _moeda = NumberFormat.currency(locale: 'pt_BR', symbol: r'R$');
final _quantidade = NumberFormat('#,##0.###', 'pt_BR');
final _quantidadeEdicao = NumberFormat('0.###', 'pt_BR');
final _percentual = NumberFormat('#,##0.#', 'pt_BR');

// O intl separa "R$" do valor com espaço não separável; normaliza para espaço comum.
String formatarMoeda(int centavos) =>
    _moeda.format(centavos / 100).replaceAll(' ', ' ');

String formatarQuantidade(num quantidade) => _quantidade.format(quantidade);

/// Sem separador de milhar, para pré-preencher campos ("1000", "2,5").
String formatarQuantidadeParaEdicao(num quantidade) => _quantidadeEdicao.format(quantidade);

String formatarPercentual(num valor) => '${_percentual.format(valor)}%';

/// Centavos contidos num texto já formatado ("R$ 12,50" -> 1250).
int centavosDoTexto(String texto) =>
    int.tryParse(texto.replaceAll(RegExp(r'\D'), '')) ?? 0;

/// Quantidade digitada, com vírgula ou ponto como decimal ("2,5" ou "2.5" -> 2.5).
/// Não há separador de milhar nos campos (ver [QuantidadeInputFormatter]).
double? quantidadeDoTexto(String texto) {
  final limpo = texto.trim().replaceAll(',', '.');
  if (limpo.isEmpty) return null;
  return double.tryParse(limpo);
}

/// Campo de dinheiro estilo app de banco: os dígitos entram pela direita
/// (digitar 1, 2, 5, 0 mostra R$ 0,01 -> R$ 0,12 -> R$ 1,25 -> R$ 12,50).
class CentavosInputFormatter extends TextInputFormatter {
  // Até R$ 99.999.999,99: mais que isso não é preço de produto, é erro de digitação.
  static const _maxDigitos = 10;

  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    var digitos = newValue.text.replaceAll(RegExp(r'\D'), '');
    if (digitos.isEmpty) {
      return const TextEditingValue();
    }
    if (digitos.length > _maxDigitos) {
      return oldValue;
    }
    final texto = formatarMoeda(int.parse(digitos));
    return TextEditingValue(
      text: texto,
      selection: TextSelection.collapsed(offset: texto.length),
    );
  }
}

/// Quantidade decimal com até 3 casas, aceitando vírgula ou ponto.
class QuantidadeInputFormatter extends TextInputFormatter {
  static final _valido = RegExp(r'^\d{0,9}([.,]\d{0,3})?$');

  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    return _valido.hasMatch(newValue.text) ? newValue : oldValue;
  }
}
