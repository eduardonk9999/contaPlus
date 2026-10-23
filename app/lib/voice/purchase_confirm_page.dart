import 'package:flutter/material.dart';

import '../core/api_client.dart';
import '../core/formatters.dart';
import '../core/uuid.dart';
import '../sales/sale_page.dart' show rotuloQuantidade;
import '../theme.dart';
import 'voice_models.dart';
import 'voice_service.dart';

/// Confere a entrada de estoque interpretada antes de gravar. Devolve o total
/// gravado (centavos) ou null se a pessoa desistir.
class PurchaseConfirmPage extends StatefulWidget {
  const PurchaseConfirmPage({
    super.key,
    required this.interpretacao,
    required this.servico,
    required this.storeId,
  });

  final Interpretacao interpretacao;
  final VoiceService servico;
  final String storeId;

  @override
  State<PurchaseConfirmPage> createState() => _PurchaseConfirmPageState();
}

class _PurchaseConfirmPageState extends State<PurchaseConfirmPage> {
  late final List<ItemInterpretado> _itens = List.of(
    widget.interpretacao.itens,
  );
  String _chave = gerarUuid();
  bool _enviando = false;

  int get _total => _itens.fold(0, (soma, item) => soma + item.totalCentavos);

  // Compra diferente, chave diferente: reenviar a mesma (rede caiu) não duplica.
  void _mudar(void Function() mudanca) => setState(() {
    mudanca();
    _chave = gerarUuid();
  });

  Future<void> _editar(int indice) async {
    final editado = await showDialog<ItemInterpretado>(
      context: context,
      builder: (_) => _EditarItemDialog(item: _itens[indice]),
    );
    if (editado != null) _mudar(() => _itens[indice] = editado);
  }

  Future<void> _confirmar() async {
    setState(() => _enviando = true);
    try {
      final total = await widget.servico.registrarCompra(
        storeId: widget.storeId,
        chaveIdempotencia: _chave,
        itens: _itens,
        origem: widget.interpretacao,
      );
      if (mounted) Navigator.of(context).pop(total);
    } on NetworkException {
      _avisarErro(
        'Sem resposta do servidor. Tente de novo: a compra não será registrada duas vezes.',
      );
    } on ApiException catch (e) {
      _avisarErro(
        e.statusCode == 403
            ? 'Só o dono da loja pode registrar compras.'
            : e.message,
      );
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  void _avisarErro(String mensagem) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(backgroundColor: AppColors.perigo, content: Text(mensagem)),
      );
  }

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;

    return Scaffold(
      appBar: AppBar(title: const Text('Entrada de estoque')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        children: [
          FraseOriginal(widget.interpretacao),
          if (widget.interpretacao.naoEncontrados.isNotEmpty)
            NaoEncontrados(widget.interpretacao.naoEncontrados),
          const SizedBox(height: 12),
          for (var i = 0; i < _itens.length; i++)
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(
                _itens[i].nome,
                style: texto.bodyLarge?.copyWith(fontWeight: FontWeight.w600),
              ),
              subtitle: Text(
                '${rotuloQuantidade(_itens[i].quantidade, _itens[i].unidade)} × '
                '${formatarMoeda(_itens[i].precoUnitarioCentavos)}',
              ),
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    formatarMoeda(_itens[i].totalCentavos),
                    style: texto.bodyLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  IconButton(
                    tooltip: 'Tirar da compra',
                    onPressed: _itens.length == 1
                        ? null
                        : () => _mudar(() => _itens.removeAt(i)),
                    icon: const Icon(Icons.close_rounded),
                  ),
                ],
              ),
              onTap: () => _editar(i),
            ),
          Text(
            'Toque num item para corrigir quantidade ou custo.',
            style: texto.bodySmall?.copyWith(color: AppColors.textoSecundario),
          ),
        ],
      ),
      bottomNavigationBar: BarraConfirmar(
        rotuloTotal: 'Total da compra',
        totalCentavos: _total,
        rotuloBotao: 'Registrar entrada',
        enviando: _enviando,
        onConfirmar: _itens.isEmpty ? null : _confirmar,
      ),
    );
  }
}

class _EditarItemDialog extends StatefulWidget {
  const _EditarItemDialog({required this.item});

  final ItemInterpretado item;

  @override
  State<_EditarItemDialog> createState() => _EditarItemDialogState();
}

class _EditarItemDialogState extends State<_EditarItemDialog> {
  late final _quantidade = TextEditingController(
    text: formatarQuantidadeParaEdicao(widget.item.quantidade),
  );
  late final _custo = TextEditingController(
    text: formatarMoeda(widget.item.precoUnitarioCentavos),
  );
  String? _erro;

  @override
  void dispose() {
    _quantidade.dispose();
    _custo.dispose();
    super.dispose();
  }

  void _salvar() {
    final quantidade = quantidadeDoTexto(_quantidade.text);
    if (quantidade == null || quantidade <= 0) {
      setState(() => _erro = 'Informe uma quantidade');
      return;
    }
    Navigator.of(context).pop(
      widget.item.copiar(
        quantidade: quantidade,
        precoUnitarioCentavos: centavosDoTexto(_custo.text),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.item.nome),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _quantidade,
            autofocus: true,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            inputFormatters: [QuantidadeInputFormatter()],
            decoration: InputDecoration(
              labelText: 'Quantidade (${widget.item.unidade.sigla})',
              errorText: _erro,
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _custo,
            keyboardType: TextInputType.number,
            inputFormatters: [CentavosInputFormatter()],
            decoration: InputDecoration(
              labelText: 'Custo por ${widget.item.unidade.sigla}',
            ),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(onPressed: _salvar, child: const Text('Salvar')),
      ],
    );
  }
}

/// A frase como foi entendida, para o lojista conferir.
class FraseOriginal extends StatelessWidget {
  const FraseOriginal(this.interpretacao, {super.key});

  final Interpretacao interpretacao;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return Container(
      margin: const EdgeInsets.only(top: 8),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.campo,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            interpretacao.fonte == FonteLancamento.voz
                ? Icons.mic_rounded
                : Icons.keyboard_rounded,
            size: 20,
            color: AppColors.azulBB,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text('“${interpretacao.frase}”', style: texto.bodyMedium),
          ),
        ],
      ),
    );
  }
}

class NaoEncontrados extends StatelessWidget {
  const NaoEncontrados(this.nomes, {super.key});

  final List<String> nomes;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(top: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.alerta.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        'Não achei no cadastro: ${nomes.join(', ')}. Cadastre em Produtos para lançar depois.',
        style: Theme.of(context).textTheme.bodyMedium
            ?.copyWith(color: AppColors.alerta),
      ),
    );
  }
}

class BarraConfirmar extends StatelessWidget {
  const BarraConfirmar({
    super.key,
    required this.rotuloTotal,
    required this.totalCentavos,
    required this.rotuloBotao,
    required this.enviando,
    required this.onConfirmar,
  });

  final String rotuloTotal;
  final int totalCentavos;
  final String rotuloBotao;
  final bool enviando;
  final VoidCallback? onConfirmar;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                Text(rotuloTotal, style: texto.titleMedium),
                const Spacer(),
                Text(
                  formatarMoeda(totalCentavos),
                  style: texto.headlineSmall?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: enviando ? null : onConfirmar,
                child: enviando
                    ? const SizedBox(
                        height: 22,
                        width: 22,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : Text(rotuloBotao),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
