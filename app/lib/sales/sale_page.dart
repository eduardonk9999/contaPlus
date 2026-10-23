import 'dart:async';

import 'package:flutter/material.dart';

import '../core/api_client.dart';
import '../core/formatters.dart';
import '../products/product_models.dart';
import '../products/product_service.dart';
import '../theme.dart';
import 'checkout_page.dart';
import 'quantity_dialog.dart';
import 'sale_models.dart';
import 'sale_service.dart';

/// Monta a venda: tocar num produto o coloca no carrinho.
class SalePage extends StatefulWidget {
  const SalePage({
    super.key,
    required this.produtos,
    required this.vendas,
    required this.cart,
    required this.storeId,
    this.onFalar,
  });

  final ProductService produtos;
  final SaleService vendas;
  final Cart cart;
  final String storeId;

  /// Abre o lançamento por voz.
  final VoidCallback? onFalar;

  @override
  State<SalePage> createState() => _SalePageState();
}

class _SalePageState extends State<SalePage> {
  static const _tamanhoPagina = 50;

  final _buscaController = TextEditingController();
  Timer? _debounce;

  String _busca = '';
  List<Product> _produtos = const [];
  bool _carregando = true;
  bool _temMais = false;
  String? _erro;
  int _geracao = 0;

  @override
  void initState() {
    super.initState();
    _buscar();
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _buscaController.dispose();
    super.dispose();
  }

  Future<void> _buscar() async {
    final geracao = ++_geracao;
    try {
      final pagina = await widget.produtos.listar(
        busca: _busca,
        tamanho: _tamanhoPagina,
        tipo: ProductType.sellable,
      );
      if (!mounted || geracao != _geracao) return;
      setState(() {
        _produtos = pagina.itens;
        _temMais = !pagina.ultima;
        _erro = null;
        _carregando = false;
      });
    } on ApiException catch (e) {
      if (!mounted || geracao != _geracao) return;
      setState(() {
        _erro = e.message;
        _carregando = false;
      });
    }
  }

  void _recarregar() {
    setState(() {
      _carregando = true;
      _erro = null;
    });
    _buscar();
  }

  void _aoDigitarBusca(String texto) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () {
      final busca = texto.trim();
      if (busca == _busca) return;
      _busca = busca;
      _recarregar();
    });
  }

  void _limparBusca() {
    _buscaController.clear();
    _debounce?.cancel();
    if (_busca.isEmpty) return;
    _busca = '';
    _recarregar();
  }

  Future<void> _tocar(Product produto) async {
    if (produto.unidade == StockUnit.unit) {
      widget.cart.adicionar(produto);
      return;
    }
    final quantidade = await pedirQuantidade(
      context,
      produto,
      atual: widget.cart.quantidadeDe(produto.id),
    );
    if (quantidade != null) widget.cart.definirQuantidade(produto, quantidade);
  }

  Future<void> _ajustarQuantidade(Product produto) async {
    final quantidade = await pedirQuantidade(
      context,
      produto,
      atual: widget.cart.quantidadeDe(produto.id),
    );
    if (quantidade != null) widget.cart.definirQuantidade(produto, quantidade);
  }

  Future<void> _cobrar() async {
    FocusScope.of(context).unfocus();
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => CheckoutPage(
          cart: widget.cart,
          vendas: widget.vendas,
          storeId: widget.storeId,
        ),
      ),
    );
    // Uma venda concluída mexe no estoque mostrado na lista.
    if (mounted) _recarregar();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Nova venda'),
        actions: [
          if (widget.onFalar != null)
            IconButton(
              tooltip: 'Vender falando',
              icon: const Icon(Icons.mic_rounded),
              onPressed: widget.onFalar,
            ),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(64),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: ValueListenableBuilder(
              valueListenable: _buscaController,
              builder: (context, valor, _) => TextField(
                controller: _buscaController,
                onChanged: _aoDigitarBusca,
                textInputAction: TextInputAction.search,
                decoration: InputDecoration(
                  hintText: 'Buscar produto para vender',
                  prefixIcon: const Icon(Icons.search_rounded),
                  contentPadding: const EdgeInsets.symmetric(vertical: 12),
                  suffixIcon: valor.text.isEmpty
                      ? null
                      : IconButton(
                          tooltip: 'Limpar busca',
                          icon: const Icon(Icons.close_rounded),
                          onPressed: _limparBusca,
                        ),
                ),
              ),
            ),
          ),
        ),
      ),
      body: ListenableBuilder(
        listenable: widget.cart,
        builder: (context, _) => Column(
          children: [
            Expanded(child: _lista()),
            if (!widget.cart.vazio)
              _BarraCarrinho(cart: widget.cart, onCobrar: _cobrar),
          ],
        ),
      ),
    );
  }

  Widget _lista() {
    final texto = Theme.of(context).textTheme;
    if (_carregando) return const Center(child: CircularProgressIndicator());
    if (_erro != null) {
      return _Mensagem(
        titulo: 'Não foi possível carregar os produtos',
        texto: _erro!,
        acao: FilledButton(
          onPressed: _recarregar,
          child: const Text('Tentar de novo'),
        ),
      );
    }
    if (_produtos.isEmpty) {
      return _Mensagem(
        titulo: _busca.isEmpty
            ? 'Nenhum produto à venda'
            : 'Nenhum produto encontrado',
        texto: _busca.isEmpty
            ? 'Cadastre os produtos que você vende na aba Produtos.'
            : 'Nada com “$_busca”. Tente outro nome.',
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.only(bottom: 16),
      itemCount: _produtos.length + (_temMais ? 1 : 0),
      separatorBuilder: (_, _) => const Divider(height: 1, indent: 76),
      itemBuilder: (context, i) {
        if (i == _produtos.length) {
          return Padding(
            padding: const EdgeInsets.all(20),
            child: Text(
              'Mostrando os $_tamanhoPagina primeiros. Use a busca para achar os outros.',
              textAlign: TextAlign.center,
              style: texto.bodySmall?.copyWith(
                color: AppColors.textoSecundario,
              ),
            ),
          );
        }
        final produto = _produtos[i];
        return _ItemVenda(
          produto: produto,
          noCarrinho: widget.cart.quantidadeDe(produto.id),
          onTap: () => _tocar(produto),
          onLongPress: () => _ajustarQuantidade(produto),
        );
      },
    );
  }
}

String rotuloQuantidade(double quantidade, StockUnit unidade) =>
    unidade == StockUnit.unit
    ? formatarQuantidade(quantidade)
    : '${formatarQuantidade(quantidade)} ${unidade.sigla}';

class _ItemVenda extends StatelessWidget {
  const _ItemVenda({
    required this.produto,
    required this.noCarrinho,
    required this.onTap,
    required this.onLongPress,
  });

  final Product produto;
  final double noCarrinho;
  final VoidCallback onTap;
  final VoidCallback onLongPress;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final preco = produto.unidade == StockUnit.unit
        ? formatarMoeda(produto.precoVendaCentavos)
        : '${formatarMoeda(produto.precoVendaCentavos)} / ${produto.unidade.sigla}';

    return ListTile(
      onTap: onTap,
      onLongPress: onLongPress,
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 4),
      leading: CircleAvatar(
        backgroundColor: AppColors.azulBB.withValues(alpha: 0.10),
        foregroundColor: AppColors.azulBB,
        child: Text(
          produto.nome.characters.first.toUpperCase(),
          style: const TextStyle(fontWeight: FontWeight.w700),
        ),
      ),
      title: Text(
        produto.nome,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: texto.bodyLarge?.copyWith(fontWeight: FontWeight.w600),
      ),
      subtitle: Text.rich(
        TextSpan(
          text: preco,
          children: [
            if (produto.semEstoque)
              const TextSpan(
                text: ' · Sem estoque',
                style: TextStyle(color: AppColors.perigo),
              ),
          ],
        ),
        style: texto.bodySmall?.copyWith(color: AppColors.textoSecundario),
      ),
      trailing: noCarrinho > 0
          ? Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                color: AppColors.azulBB,
                borderRadius: BorderRadius.circular(20),
              ),
              child: Text(
                rotuloQuantidade(noCarrinho, produto.unidade),
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w700,
                ),
              ),
            )
          : const Icon(
              Icons.add_circle_outline_rounded,
              color: AppColors.azulBB,
            ),
    );
  }
}

class _BarraCarrinho extends StatelessWidget {
  const _BarraCarrinho({required this.cart, required this.onCobrar});

  final Cart cart;
  final VoidCallback onCobrar;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final itens = cart.itens.length;

    return Material(
      elevation: 8,
      color: AppColors.branco,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 12, 16, 12),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    itens == 1 ? '1 item' : '$itens itens',
                    style: texto.bodySmall?.copyWith(
                      color: AppColors.textoSecundario,
                    ),
                  ),
                  Text(
                    formatarMoeda(cart.totalCentavos),
                    style: texto.titleLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
            ),
            FilledButton.icon(
              style: FilledButton.styleFrom(minimumSize: const Size(150, 52)),
              onPressed: onCobrar,
              icon: const Icon(Icons.point_of_sale_rounded),
              label: const Text('Cobrar'),
            ),
          ],
        ),
      ),
    );
  }
}

class _Mensagem extends StatelessWidget {
  const _Mensagem({required this.titulo, required this.texto, this.acao});

  final String titulo;
  final String texto;
  final Widget? acao;

  @override
  Widget build(BuildContext context) {
    final estilo = Theme.of(context).textTheme;
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.point_of_sale_outlined,
              size: 56,
              color: AppColors.textoSuave,
            ),
            const SizedBox(height: 16),
            Text(
              titulo,
              textAlign: TextAlign.center,
              style: estilo.titleMedium?.copyWith(fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            Text(
              texto,
              textAlign: TextAlign.center,
              style: estilo.bodyMedium?.copyWith(
                color: AppColors.textoSecundario,
              ),
            ),
            if (acao != null) ...[const SizedBox(height: 24), acao!],
          ],
        ),
      ),
    );
  }
}
