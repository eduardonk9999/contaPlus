import 'package:contaplus/core/formatters.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

String _digitar(TextInputFormatter formatter, List<String> teclas) {
  var valor = TextEditingValue.empty;
  for (final tecla in teclas) {
    final novo = TextEditingValue(text: valor.text + tecla);
    valor = formatter.formatEditUpdate(valor, novo);
  }
  return valor.text;
}

void main() {
  test('formata centavos como reais', () {
    expect(formatarMoeda(0), 'R\$ 0,00');
    expect(formatarMoeda(1250), 'R\$ 12,50');
    expect(formatarMoeda(123456789), 'R\$ 1.234.567,89');
  });

  test('lê centavos de um texto formatado', () {
    expect(centavosDoTexto('R\$ 1.234,56'), 123456);
    expect(centavosDoTexto(''), 0);
  });

  test('campo de dinheiro preenche da direita para a esquerda', () {
    expect(_digitar(CentavosInputFormatter(), ['1', '2', '5', '0']), 'R\$ 12,50');
  });

  test('campo de dinheiro apagado fica vazio', () {
    final formatter = CentavosInputFormatter();
    final resultado = formatter.formatEditUpdate(
      const TextEditingValue(text: 'R\$ 0,05'),
      const TextEditingValue(text: 'R\$ 0,0'),
    );
    expect(resultado.text, 'R\$ 0,00');
    expect(
      formatter.formatEditUpdate(resultado, const TextEditingValue(text: '')).text,
      '',
    );
  });

  test('quantidade aceita vírgula ou ponto como decimal', () {
    expect(quantidadeDoTexto('2,5'), 2.5);
    expect(quantidadeDoTexto('2.5'), 2.5);
    expect(quantidadeDoTexto('1000'), 1000);
    expect(quantidadeDoTexto(''), isNull);
  });

  test('quantidade para edição não usa separador de milhar', () {
    expect(formatarQuantidadeParaEdicao(1000), '1000');
    expect(formatarQuantidadeParaEdicao(2.5), '2,5');
    expect(quantidadeDoTexto(formatarQuantidadeParaEdicao(1234.5)), 1234.5);
  });

  test('campo de quantidade recusa mais de 3 casas e letras', () {
    final formatter = QuantidadeInputFormatter();
    expect(_digitar(formatter, ['2', ',', '1', '2', '3', '4']), '2,123');
    expect(_digitar(formatter, ['1', 'a']), '1');
  });
}
