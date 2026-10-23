import 'package:flutter/material.dart';

import '../core/formatters.dart';
import '../products/product_models.dart';

/// Pergunta a quantidade de um item vendido por peso/volume (ex.: 0,350 kg).
Future<double?> pedirQuantidade(BuildContext context, Product produto, {double? atual}) {
  return showDialog<double>(
    context: context,
    builder: (_) => _QuantidadeDialog(produto: produto, atual: atual),
  );
}

class _QuantidadeDialog extends StatefulWidget {
  const _QuantidadeDialog({required this.produto, this.atual});

  final Product produto;
  final double? atual;

  @override
  State<_QuantidadeDialog> createState() => _QuantidadeDialogState();
}

class _QuantidadeDialogState extends State<_QuantidadeDialog> {
  late final _controller = TextEditingController(
    text: (widget.atual ?? 0) > 0 ? formatarQuantidadeParaEdicao(widget.atual!) : '',
  );
  String? _erro;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _confirmar() {
    final quantidade = quantidadeDoTexto(_controller.text);
    if (quantidade == null || quantidade <= 0) {
      setState(() => _erro = 'Informe uma quantidade');
      return;
    }
    Navigator.of(context).pop(quantidade);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.produto.nome),
      content: TextField(
        controller: _controller,
        autofocus: true,
        keyboardType: const TextInputType.numberWithOptions(decimal: true),
        inputFormatters: [QuantidadeInputFormatter()],
        onSubmitted: (_) => _confirmar(),
        decoration: InputDecoration(
          labelText: 'Quantidade',
          suffixText: widget.produto.unidade.sigla,
          errorText: _erro,
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Cancelar')),
        FilledButton(
          style: FilledButton.styleFrom(minimumSize: const Size(0, 44)),
          onPressed: _confirmar,
          child: const Text('OK'),
        ),
      ],
    );
  }
}
