import 'dart:async';

import 'package:flutter/material.dart';

import '../core/api_client.dart';
import '../core/formatters.dart';
import '../theme.dart';
import 'product_form_page.dart';
import 'product_models.dart';
import 'product_service.dart';

class ProductsPage extends StatefulWidget {
  const ProductsPage({super.key, required this.service, required this.storeId});

  final ProductService service;
  final String storeId;

  @override
  State<ProductsPage> createState() => _ProductsPageState();
}

class _ProductsPageState extends State<ProductsPage> {
  final _scroll = ScrollController();
  final _buscaController = TextEditingController();
  final _produtos = <Product>[];
  Map<String, Category> _categorias = const {};
  Timer? _debounce;

  String _busca = '';
  int _proximaPagina = 0;
  bool _ultimaPagina = false;

  /// Buscando a primeira página (abertura, busca nova, puxar para atualizar).
  bool _recarregando = true;

  /// Buscando a próxima página no fim da rolagem (só trava chamadas repetidas).
  bool _carregandoMais = false;

  String? _erro;
  bool _erroAoCarregarMais = false;

  // Cada recarga ganha um número; respostas de recargas anteriores são descartadas
  // (ex.: a pessoa digitou outra busca antes da anterior responder).
  int _geracao = 0;

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_aoRolar);
    _carregarCategorias();
    _buscarPrimeiraPagina();
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _scroll.dispose();
    _buscaController.dispose();
    super.dispose();
  }

  Future<void> _carregarCategorias() async {
    try {
      final categorias = await widget.service.categorias();
      if (mounted) setState(() => _categorias = {for (final c in categorias) c.id: c});
    } on ApiException {
      // A categoria é só um rótulo na lista; sem ela a tela continua útil.
    }
  }

  Future<void> _recarregar() {
    setState(() {
      _recarregando = true;
      _erro = null;
    });
    return _buscarPrimeiraPagina();
  }

  Future<void> _buscarPrimeiraPagina() async {
    final geracao = ++_geracao;
    _carregandoMais = false;
    _erroAoCarregarMais = false;
    try {
      final pagina = await widget.service.listar(busca: _busca);
      if (!mounted || geracao != _geracao) return;
      setState(() {
        _produtos
          ..clear()
          ..addAll(pagina.itens);
        _proximaPagina = pagina.pagina + 1;
        _ultimaPagina = pagina.ultima;
      });
    } on ApiException catch (e) {
      if (!mounted || geracao != _geracao) return;
      if (_produtos.isEmpty) {
        setState(() => _erro = e.message);
      } else {
        _avisar(e.message);
      }
    } finally {
      if (mounted && geracao == _geracao) setState(() => _recarregando = false);
    }
  }

  Future<void> _buscarProximaPagina() async {
    if (_recarregando || _carregandoMais || _ultimaPagina) return;
    final geracao = _geracao;
    setState(() {
      _carregandoMais = true;
      _erroAoCarregarMais = false;
    });
    try {
      final pagina = await widget.service.listar(busca: _busca, pagina: _proximaPagina);
      if (!mounted || geracao != _geracao) return;
      setState(() {
        _produtos.addAll(pagina.itens);
        _proximaPagina = pagina.pagina + 1;
        _ultimaPagina = pagina.ultima;
      });
    } on ApiException {
      if (!mounted || geracao != _geracao) return;
      setState(() => _erroAoCarregarMais = true);
    } finally {
      if (mounted && geracao == _geracao) setState(() => _carregandoMais = false);
    }
  }

  void _aoRolar() {
    if (_scroll.position.extentAfter < 400 && !_erroAoCarregarMais) {
      _buscarProximaPagina();
    }
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

  Future<void> _abrirFormulario([Product? produto]) async {
    // Senão, ao voltar, o Flutter devolve o foco à busca e o teclado reabre sozinho.
    FocusScope.of(context).unfocus();
    final mensagem = await Navigator.of(context).push<String>(
      MaterialPageRoute(
        builder: (_) => ProductFormPage(
          service: widget.service,
          storeId: widget.storeId,
          categorias: _categorias.values.toList(),
          produto: produto,
        ),
      ),
    );
    if (mensagem == null || !mounted) return;
    _avisar(mensagem);
    _recarregar();
  }

  void _avisar(String mensagem) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(mensagem)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Produtos'),
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
                  hintText: 'Buscar produto',
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
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _abrirFormulario(),
        icon: const Icon(Icons.add_rounded),
        label: const Text('Novo produto'),
      ),
      body: _conteudo(),
    );
  }

  Widget _conteudo() {
    if (_produtos.isEmpty) {
      if (_recarregando) {
        return const Center(child: CircularProgressIndicator());
      }
      if (_erro != null) {
        return _Aviso(
          icone: Icons.cloud_off_rounded,
          titulo: 'Não foi possível carregar os produtos',
          texto: _erro!,
          acao: FilledButton(onPressed: _recarregar, child: const Text('Tentar de novo')),
        );
      }
      if (_busca.isNotEmpty) {
        return _Aviso(
          icone: Icons.search_off_rounded,
          titulo: 'Nenhum produto encontrado',
          texto: 'Nada com “$_busca”. Tente outro nome.',
        );
      }
      return _Aviso(
        icone: Icons.inventory_2_outlined,
        titulo: 'Nenhum produto cadastrado',
        texto: 'Cadastre o que você vende para usar nas vendas e acompanhar o estoque.',
        acao: FilledButton.icon(
          onPressed: () => _abrirFormulario(),
          icon: const Icon(Icons.add_rounded),
          label: const Text('Cadastrar produto'),
        ),
      );
    }

    return Column(
      children: [
        if (_recarregando) const LinearProgressIndicator(minHeight: 2),
        Expanded(
          child: RefreshIndicator(
            onRefresh: _recarregar,
            child: ListView.separated(
              controller: _scroll,
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.only(bottom: 96),
              itemCount: _produtos.length + 1,
              separatorBuilder: (_, _) => const Divider(height: 1, indent: 76),
              itemBuilder: (context, i) {
                if (i == _produtos.length) return _rodape();
                final produto = _produtos[i];
                return _ProdutoTile(
                  produto: produto,
                  categoria: _categorias[produto.categoriaId]?.nome,
                  onTap: () => _abrirFormulario(produto),
                );
              },
            ),
          ),
        ),
      ],
    );
  }

  Widget _rodape() {
    if (_erroAoCarregarMais) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Center(
          child: TextButton.icon(
            onPressed: _buscarProximaPagina,
            icon: const Icon(Icons.refresh_rounded),
            label: const Text('Erro ao carregar mais. Tentar de novo'),
          ),
        ),
      );
    }
    if (!_ultimaPagina) {
      return const Padding(
        padding: EdgeInsets.all(20),
        child: Center(child: CircularProgressIndicator(strokeWidth: 2.5)),
      );
    }
    return const SizedBox.shrink();
  }
}

class _ProdutoTile extends StatelessWidget {
  const _ProdutoTile({required this.produto, required this.categoria, required this.onTap});

  final Product produto;
  final String? categoria;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final estoque = '${formatarQuantidade(produto.estoque)} ${produto.unidade.sigla}';
    final detalhes = [
      if (produto.tipo == ProductType.supply) produto.tipo.rotulo,
      ?categoria,
    ];

    final (textoEstoque, corEstoque) = switch (produto) {
      Product(semEstoque: true) => ('Sem estoque', AppColors.perigo),
      Product(estoqueBaixo: true) => ('Estoque baixo · $estoque', AppColors.alerta),
      _ => ('Estoque: $estoque', AppColors.textoSecundario),
    };

    return ListTile(
      onTap: onTap,
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
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          if (detalhes.isNotEmpty)
            Text(
              detalhes.join(' · '),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: texto.bodySmall?.copyWith(color: AppColors.textoSecundario),
            ),
          Text(textoEstoque, style: texto.bodySmall?.copyWith(color: corEstoque)),
        ],
      ),
      trailing: produto.precoVendaCentavos > 0
          ? Text(
              formatarMoeda(produto.precoVendaCentavos),
              style: texto.titleSmall?.copyWith(fontWeight: FontWeight.w700),
            )
          : Text(
              'Custo ${formatarMoeda(produto.custoCentavos)}',
              style: texto.bodySmall?.copyWith(color: AppColors.textoSecundario),
            ),
    );
  }
}

class _Aviso extends StatelessWidget {
  const _Aviso({required this.icone, required this.titulo, required this.texto, this.acao});

  final IconData icone;
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
            Icon(icone, size: 56, color: AppColors.textoSuave),
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
              style: estilo.bodyMedium?.copyWith(color: AppColors.textoSecundario),
            ),
            if (acao != null) ...[const SizedBox(height: 24), acao!],
          ],
        ),
      ),
    );
  }
}
