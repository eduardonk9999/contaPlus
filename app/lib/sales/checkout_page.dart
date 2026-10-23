import 'dart:async';

import 'package:flutter/material.dart';

import '../core/api_client.dart';
import '../core/formatters.dart';
import '../products/product_models.dart';
import '../theme.dart';
import 'quantity_dialog.dart';
import 'sale_done_page.dart';
import 'sale_models.dart';
import 'sale_page.dart';
import 'sale_service.dart';

/// Revisa os itens, escolhe a forma de pagamento (com troco no dinheiro) e confirma.
class CheckoutPage extends StatefulWidget {
  const CheckoutPage({
    super.key,
    required this.cart,
    required this.vendas,
    required this.storeId,
  });

  final Cart cart;
  final SaleService vendas;
  final String storeId;

  @override
  State<CheckoutPage> createState() => _CheckoutPageState();
}

class _CheckoutPageState extends State<CheckoutPage> {
  final _recebido = TextEditingController();
  Timer? _debounce;

  SalePreview? _preview;
  bool _previewDesatualizado = true;
  String? _erroPreview;
  int _geracaoPreview = 0;

  List<PaymentMethod>? _formas;
  String? _erroFormas;
  PaymentMethod? _forma;

  bool _enviando = false;
  bool _concluida = false;

  Cart get _cart => widget.cart;

  @override
  void initState() {
    super.initState();
    _cart.addListener(_carrinhoMudou);
    _recebido.addListener(_atualizar);
    _carregarFormas();
    _atualizarPreview();
  }

  @override
  void dispose() {
    _cart.removeListener(_carrinhoMudou);
    _debounce?.cancel();
    _recebido.dispose();
    super.dispose();
  }

  void _atualizar() => setState(() {});

  void _carrinhoMudou() {
    if (_concluida) return;
    if (_cart.vazio) {
      Navigator.of(context).pop();
      return;
    }
    setState(() => _previewDesatualizado = true);
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 300), _atualizarPreview);
  }

  // Os totais oficiais vêm da API (preço pode ter mudado desde que a lista carregou).
  Future<void> _atualizarPreview() async {
    final geracao = ++_geracaoPreview;
    try {
      final preview = await widget.vendas.preview(widget.storeId, _cart.itens);
      if (!mounted || geracao != _geracaoPreview) return;
      setState(() {
        _preview = preview;
        _previewDesatualizado = false;
        _erroPreview = null;
      });
    } on ApiException catch (e) {
      if (!mounted || geracao != _geracaoPreview) return;
      setState(() => _erroPreview = e.message);
    }
  }

  Future<void> _carregarFormas() async {
    try {
      final formas = await widget.vendas.formasPagamento();
      if (mounted) setState(() => _formas = formas);
    } on ApiException catch (e) {
      if (mounted) setState(() => _erroFormas = e.message);
    }
  }

  bool get _previewPronto => _preview != null && !_previewDesatualizado;

  int get _total => _previewPronto ? _preview!.totalCentavos : _cart.totalCentavos;

  /// Valor entregue em dinheiro; vazio significa "valor exato".
  int? get _recebidoCentavos =>
      _forma?.aceitaTroco == true && _recebido.text.isNotEmpty ? centavosDoTexto(_recebido.text) : null;

  int get _troco => (_recebidoCentavos ?? _total) - _total;

  bool get _podeConfirmar => !_enviando && _previewPronto && _forma != null && _troco >= 0;

  Future<void> _confirmar() async {
    final forma = _forma!;
    final total = _preview!.totalCentavos;
    final pago = _recebidoCentavos ?? total;
    final troco = pago - total;

    setState(() => _enviando = true);
    try {
      final venda = await widget.vendas.confirmar(
        storeId: widget.storeId,
        chaveIdempotencia: _cart.chaveIdempotencia,
        itens: _cart.itens,
        forma: forma,
        valorPagoCentavos: pago,
        trocoCentavos: troco,
      );
      if (!mounted) return;
      _concluida = true;
      _cart.limpar();
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => SaleDonePage(
            totalCentavos: venda.totalCentavos,
            trocoCentavos: troco,
            formaPagamento: forma.nome,
          ),
        ),
      );
    } on NetworkException {
      // A chave de idempotência continua a mesma enquanto o carrinho não muda.
      _avisarErro('Sem resposta do servidor. Tente de novo: a venda não será registrada duas vezes.');
    } on ApiException catch (e) {
      _avisarErro(e.message);
    } finally {
      if (mounted && !_concluida) setState(() => _enviando = false);
    }
  }

  void _avisarErro(String mensagem) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(backgroundColor: Colors.red.shade700, content: Text(mensagem)));
  }

  Future<void> _editarQuantidade(CartItem item) async {
    final quantidade = await pedirQuantidade(context, item.produto, atual: item.quantidade);
    if (quantidade != null) _cart.definirQuantidade(item.produto, quantidade);
  }

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final avisos = _previewPronto ? _preview!.avisos : const <String>[];

    return Scaffold(
      appBar: AppBar(title: const Text('Cobrar')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        children: [
          const _Secao('Itens'),
          for (final item in _cart.itens)
            _LinhaItem(
              item: item,
              onMenos: () => _cart.definirQuantidade(item.produto, item.quantidade - 1),
              onMais: () => _cart.adicionar(item.produto),
              onEditar: () => _editarQuantidade(item),
            ),
          if (avisos.isNotEmpty) _Avisos(avisos),
          if (_erroPreview != null)
            _Falha(mensagem: _erroPreview!, onTentarDeNovo: _atualizarPreview),
          const _Secao('Pagamento'),
          if (_erroFormas != null)
            _Falha(mensagem: _erroFormas!, onTentarDeNovo: () {
              setState(() => _erroFormas = null);
              _carregarFormas();
            })
          else if (_formas == null)
            const LinearProgressIndicator(minHeight: 2)
          else if (_formas!.isEmpty)
            Text(
              'Nenhuma forma de pagamento ativa na loja.',
              style: texto.bodyMedium?.copyWith(color: AppColors.textoSecundario),
            )
          else
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final forma in _formas!)
                  ChoiceChip(
                    avatar: Icon(_iconeDe(forma.tipo), size: 18),
                    label: Text(forma.nome),
                    selected: _forma?.id == forma.id,
                    onSelected: (_) => setState(() {
                      _forma = forma;
                      _recebido.clear();
                    }),
                  ),
              ],
            ),
          if (_forma?.aceitaTroco == true) ...[
            const SizedBox(height: 16),
            TextField(
              controller: _recebido,
              keyboardType: TextInputType.number,
              inputFormatters: [CentavosInputFormatter()],
              decoration: const InputDecoration(
                labelText: 'Valor recebido',
                helperText: 'Deixe vazio se for o valor exato.',
              ),
            ),
            if (_recebidoCentavos != null) _Troco(diferencaCentavos: _troco),
          ],
        ],
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                children: [
                  Text('Total', style: texto.titleMedium),
                  const Spacer(),
                  Text(
                    formatarMoeda(_total),
                    style: texto.headlineSmall?.copyWith(fontWeight: FontWeight.w700),
                  ),
                ],
              ),
              if (_previewPronto)
                Align(
                  alignment: Alignment.centerRight,
                  child: Text(
                    'Lucro estimado ${formatarMoeda(_preview!.lucroCentavos)} · '
                    'margem ${formatarPercentual(_preview!.margemPercentual)}',
                    style: texto.bodySmall?.copyWith(color: AppColors.sucesso),
                  ),
                ),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: _podeConfirmar ? _confirmar : null,
                child: _enviando
                    ? const SizedBox(
                        height: 22,
                        width: 22,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                      )
                    : Text(_forma == null ? 'Escolha a forma de pagamento' : 'Confirmar venda'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

IconData _iconeDe(PaymentType tipo) => switch (tipo) {
      PaymentType.cash => Icons.payments_outlined,
      PaymentType.pix => Icons.qr_code_2_rounded,
      PaymentType.debitCard || PaymentType.creditCard => Icons.credit_card_rounded,
      PaymentType.transfer => Icons.account_balance_outlined,
      PaymentType.other => Icons.more_horiz_rounded,
    };

class _Secao extends StatelessWidget {
  const _Secao(this.titulo);

  final String titulo;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 20, bottom: 8),
      child: Text(
        titulo,
        style: Theme.of(context).textTheme.labelLarge?.copyWith(
              color: AppColors.textoSecundario,
              fontWeight: FontWeight.w600,
            ),
      ),
    );
  }
}

class _LinhaItem extends StatelessWidget {
  const _LinhaItem({
    required this.item,
    required this.onMenos,
    required this.onMais,
    required this.onEditar,
  });

  final CartItem item;
  final VoidCallback onMenos;
  final VoidCallback onMais;
  final VoidCallback onEditar;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final produto = item.produto;
    final porUnidade = produto.unidade == StockUnit.unit;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  produto.nome,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: texto.bodyLarge?.copyWith(fontWeight: FontWeight.w600),
                ),
                Text(
                  porUnidade
                      ? formatarMoeda(produto.precoVendaCentavos)
                      : '${formatarMoeda(produto.precoVendaCentavos)} / ${produto.unidade.sigla}',
                  style: texto.bodySmall?.copyWith(color: AppColors.textoSecundario),
                ),
              ],
            ),
          ),
          if (porUnidade) ...[
            IconButton(
              tooltip: 'Diminuir',
              onPressed: onMenos,
              icon: const Icon(Icons.remove_circle_outline_rounded),
            ),
            Text(formatarQuantidade(item.quantidade), style: texto.titleMedium),
            IconButton(
              tooltip: 'Aumentar',
              onPressed: onMais,
              icon: const Icon(Icons.add_circle_outline_rounded),
            ),
          ] else
            TextButton.icon(
              onPressed: onEditar,
              icon: const Icon(Icons.edit_outlined, size: 18),
              label: Text(rotuloQuantidade(item.quantidade, produto.unidade)),
            ),
          SizedBox(
            width: 92,
            child: Text(
              formatarMoeda(item.totalCentavos),
              textAlign: TextAlign.right,
              style: texto.bodyLarge?.copyWith(fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
  }
}

class _Avisos extends StatelessWidget {
  const _Avisos(this.avisos);

  final List<String> avisos;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(top: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.alerta.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (final aviso in avisos)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 2),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.warning_amber_rounded, size: 18, color: AppColors.alerta),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(aviso, style: const TextStyle(color: AppColors.alerta)),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

class _Falha extends StatelessWidget {
  const _Falha({required this.mensagem, required this.onTentarDeNovo});

  final String mensagem;
  final VoidCallback onTentarDeNovo;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Row(
        children: [
          Expanded(
            child: Text(mensagem, style: const TextStyle(color: AppColors.perigo)),
          ),
          TextButton(onPressed: onTentarDeNovo, child: const Text('Tentar de novo')),
        ],
      ),
    );
  }
}

/// Troco a devolver, ou quanto ainda falta receber.
class _Troco extends StatelessWidget {
  const _Troco({required this.diferencaCentavos});

  final int diferencaCentavos;

  @override
  Widget build(BuildContext context) {
    final falta = diferencaCentavos < 0;
    final cor = falta ? AppColors.perigo : AppColors.sucesso;
    final texto = Theme.of(context).textTheme;

    return Container(
      margin: const EdgeInsets.only(top: 12),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: cor.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          Text(falta ? 'Faltam' : 'Troco', style: texto.titleMedium?.copyWith(color: cor)),
          const Spacer(),
          Text(
            formatarMoeda(diferencaCentavos.abs()),
            style: texto.titleLarge?.copyWith(color: cor, fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}
