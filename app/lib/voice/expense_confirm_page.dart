import 'package:flutter/material.dart';

import '../core/api_client.dart';
import '../core/formatters.dart';
import '../core/uuid.dart';
import '../theme.dart';
import 'purchase_confirm_page.dart';
import 'voice_models.dart';
import 'voice_service.dart';

/// Confere a despesa interpretada antes de gravar. Devolve true se gravou.
class ExpenseConfirmPage extends StatefulWidget {
  const ExpenseConfirmPage({
    super.key,
    required this.interpretacao,
    required this.servico,
    required this.storeId,
  });

  final Interpretacao interpretacao;
  final VoiceService servico;
  final String storeId;

  @override
  State<ExpenseConfirmPage> createState() => _ExpenseConfirmPageState();
}

class _ExpenseConfirmPageState extends State<ExpenseConfirmPage> {
  late final DespesaInterpretada _despesa = widget.interpretacao.despesa!;
  late final _descricao = TextEditingController(text: _despesa.descricao);
  late final _valor = TextEditingController(
    text: formatarMoeda(_despesa.valorCentavos),
  );
  late ExpenseCategory _categoria = _despesa.categoria;
  String _chave = gerarUuid();
  bool _enviando = false;

  @override
  void initState() {
    super.initState();
    // Qualquer correção vira outra despesa: troca a chave de idempotência.
    void mudou() => setState(() => _chave = gerarUuid());
    _descricao.addListener(mudou);
    _valor.addListener(mudou);
  }

  @override
  void dispose() {
    _descricao.dispose();
    _valor.dispose();
    super.dispose();
  }

  int get _valorCentavos => centavosDoTexto(_valor.text);

  bool get _valido => _descricao.text.trim().isNotEmpty && _valorCentavos > 0;

  Future<void> _confirmar() async {
    FocusScope.of(context).unfocus();
    setState(() => _enviando = true);
    try {
      await widget.servico.registrarDespesa(
        storeId: widget.storeId,
        chaveIdempotencia: _chave,
        descricao: _descricao.text.trim(),
        valorCentavos: _valorCentavos,
        categoria: _categoria,
        origem: widget.interpretacao,
      );
      if (mounted) Navigator.of(context).pop(true);
    } on NetworkException {
      _avisarErro(
        'Sem resposta do servidor. Tente de novo: a despesa não será registrada duas vezes.',
      );
    } on ApiException catch (e) {
      _avisarErro(
        e.statusCode == 403
            ? 'Só o dono da loja pode registrar despesas.'
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
      appBar: AppBar(title: const Text('Despesa')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        children: [
          FraseOriginal(widget.interpretacao),
          const SizedBox(height: 20),
          TextField(
            controller: _descricao,
            textCapitalization: TextCapitalization.sentences,
            decoration: const InputDecoration(labelText: 'Descrição'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _valor,
            keyboardType: TextInputType.number,
            inputFormatters: [CentavosInputFormatter()],
            decoration: const InputDecoration(labelText: 'Valor'),
          ),
          const SizedBox(height: 20),
          Text(
            'Categoria',
            style: texto.labelLarge?.copyWith(
              color: AppColors.textoSecundario,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final categoria in ExpenseCategory.values)
                ChoiceChip(
                  label: Text(categoria.rotulo),
                  selected: _categoria == categoria,
                  onSelected: (_) => setState(() {
                    _categoria = categoria;
                    _chave = gerarUuid();
                  }),
                ),
            ],
          ),
        ],
      ),
      bottomNavigationBar: BarraConfirmar(
        rotuloTotal: 'Valor',
        totalCentavos: _valorCentavos,
        rotuloBotao: 'Registrar despesa',
        enviando: _enviando,
        onConfirmar: _valido ? _confirmar : null,
      ),
    );
  }
}
