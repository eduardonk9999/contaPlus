import 'package:flutter/material.dart';

import '../core/api_client.dart';
import '../core/formatters.dart';
import '../theme.dart';
import 'product_models.dart';
import 'product_service.dart';

/// Cadastro (sem [produto]) ou edição de um produto.
/// Ao salvar, fecha devolvendo a mensagem de confirmação para a lista exibir.
class ProductFormPage extends StatefulWidget {
  const ProductFormPage({
    super.key,
    required this.service,
    required this.storeId,
    required this.categorias,
    this.produto,
  });

  final ProductService service;
  final String storeId;
  final List<Category> categorias;
  final Product? produto;

  @override
  State<ProductFormPage> createState() => _ProductFormPageState();
}

class _ProductFormPageState extends State<ProductFormPage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nome;
  late final TextEditingController _preco;
  late final TextEditingController _custo;
  late final TextEditingController _minimo;
  late ProductType _tipo;
  late StockUnit _unidade;
  String? _categoriaId;
  bool _salvando = false;

  bool get _editando => widget.produto != null;

  @override
  void initState() {
    super.initState();
    final p = widget.produto;
    _nome = TextEditingController(text: p?.nome ?? '');
    _preco = TextEditingController(text: p == null ? '' : formatarMoeda(p.precoVendaCentavos));
    _custo = TextEditingController(text: p == null ? '' : formatarMoeda(p.custoCentavos));
    _minimo = TextEditingController(
      text: p == null || p.estoqueMinimo == 0 ? '' : formatarQuantidadeParaEdicao(p.estoqueMinimo),
    );
    _tipo = p?.tipo ?? ProductType.sellable;
    _unidade = p?.unidade ?? StockUnit.unit;
    _categoriaId = p?.categoriaId;
    _preco.addListener(_atualizarMargem);
    _custo.addListener(_atualizarMargem);
  }

  @override
  void dispose() {
    _nome.dispose();
    _preco.dispose();
    _custo.dispose();
    _minimo.dispose();
    super.dispose();
  }

  void _atualizarMargem() => setState(() {});

  Future<void> _salvar() async {
    if (!_formKey.currentState!.validate()) return;
    final dados = ProductInput(
      nome: _nome.text.trim(),
      tipo: _tipo,
      custoCentavos: centavosDoTexto(_custo.text),
      precoVendaCentavos: centavosDoTexto(_preco.text),
      unidade: _unidade,
      categoriaId: _categoriaId,
      estoqueMinimo: quantidadeDoTexto(_minimo.text) ?? 0,
    );

    setState(() => _salvando = true);
    try {
      if (_editando) {
        await widget.service.atualizar(widget.produto!.id, dados);
      } else {
        await widget.service.criar(widget.storeId, dados);
      }
      if (!mounted) return;
      Navigator.of(context).pop(_editando ? 'Produto atualizado' : 'Produto cadastrado');
    } on ApiException catch (e) {
      _avisarErro(e.message);
    } finally {
      if (mounted) setState(() => _salvando = false);
    }
  }

  Future<void> _desativar() async {
    final confirmou = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Desativar produto?'),
        content: Text(
          '“${widget.produto!.nome}” deixa de aparecer na lista e nas vendas. '
          'O histórico de vendas continua guardado.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.perigo,
              minimumSize: const Size(0, 44),
            ),
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Desativar'),
          ),
        ],
      ),
    );
    if (confirmou != true || !mounted) return;

    setState(() => _salvando = true);
    try {
      await widget.service.desativar(widget.produto!.id);
      if (!mounted) return;
      Navigator.of(context).pop('Produto desativado');
    } on ApiException catch (e) {
      _avisarErro(e.message);
    } finally {
      if (mounted) setState(() => _salvando = false);
    }
  }

  void _avisarErro(String mensagem) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(backgroundColor: Colors.red.shade700, content: Text(mensagem)));
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.produto;
    final categoriaDesconhecida = _categoriaId != null &&
        !widget.categorias.any((c) => c.id == _categoriaId);

    return Scaffold(
      appBar: AppBar(title: Text(_editando ? 'Editar produto' : 'Novo produto')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
          children: [
            const _Secao('Produto'),
            TextFormField(
              controller: _nome,
              textCapitalization: TextCapitalization.sentences,
              textInputAction: TextInputAction.next,
              decoration: const InputDecoration(labelText: 'Nome'),
              validator: (v) =>
                  (v ?? '').trim().isEmpty ? 'Informe o nome do produto' : null,
            ),
            const SizedBox(height: 14),
            SegmentedButton<ProductType>(
              showSelectedIcon: false,
              segments: const [
                ButtonSegment(
                  value: ProductType.sellable,
                  label: Text('Para venda'),
                  icon: Icon(Icons.sell_outlined),
                ),
                ButtonSegment(
                  value: ProductType.supply,
                  label: Text('Insumo'),
                  icon: Icon(Icons.inventory_outlined),
                ),
              ],
              selected: {_tipo},
              onSelectionChanged: (s) => setState(() => _tipo = s.first),
            ),
            const SizedBox(height: 14),
            DropdownButtonFormField<String?>(
              initialValue: _categoriaId,
              decoration: const InputDecoration(labelText: 'Categoria'),
              items: [
                const DropdownMenuItem(value: null, child: Text('Sem categoria')),
                for (final c in widget.categorias)
                  DropdownMenuItem(value: c.id, child: Text(c.nome)),
                // A categoria do produto pode ter sido desativada: mantém para não perdê-la.
                if (categoriaDesconhecida)
                  DropdownMenuItem(value: _categoriaId, child: const Text('Categoria inativa')),
              ],
              onChanged: (v) => setState(() => _categoriaId = v),
            ),
            const _Secao('Preço'),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: TextFormField(
                    controller: _preco,
                    keyboardType: TextInputType.number,
                    inputFormatters: [CentavosInputFormatter()],
                    // Insumo (ex.: farinha) é matéria-prima: pode não ter preço de venda.
                    decoration: InputDecoration(
                      labelText: _tipo == ProductType.supply
                          ? 'Preço de venda (opcional)'
                          : 'Preço de venda',
                    ),
                    validator: (v) =>
                        _tipo == ProductType.sellable && centavosDoTexto(v ?? '') == 0
                            ? 'Informe o preço'
                            : null,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: TextFormField(
                    controller: _custo,
                    keyboardType: TextInputType.number,
                    inputFormatters: [CentavosInputFormatter()],
                    decoration: const InputDecoration(
                      labelText: 'Custo',
                      hintText: 'R\$ 0,00',
                    ),
                  ),
                ),
              ],
            ),
            _Margem(
              precoCentavos: centavosDoTexto(_preco.text),
              custoCentavos: centavosDoTexto(_custo.text),
              unidade: _unidade,
            ),
            const _Secao('Estoque'),
            DropdownButtonFormField<StockUnit>(
              initialValue: _unidade,
              decoration: const InputDecoration(labelText: 'Unidade de medida'),
              items: [
                for (final u in StockUnit.values)
                  DropdownMenuItem(value: u, child: Text('${u.nome} (${u.sigla})')),
              ],
              onChanged: (u) => setState(() => _unidade = u ?? _unidade),
            ),
            const SizedBox(height: 14),
            TextFormField(
              controller: _minimo,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: [QuantidadeInputFormatter()],
              decoration: InputDecoration(
                labelText: 'Estoque mínimo (opcional)',
                suffixText: _unidade.sigla,
                helperText: 'Avisamos quando o estoque chegar nesse número.',
              ),
            ),
            if (p != null) ...[
              const SizedBox(height: 16),
              _EstoqueAtual(produto: p, unidade: _unidade),
              const SizedBox(height: 24),
              Center(
                child: TextButton.icon(
                  style: TextButton.styleFrom(foregroundColor: AppColors.perigo),
                  onPressed: _salvando ? null : _desativar,
                  icon: const Icon(Icons.block_rounded),
                  label: const Text('Desativar produto'),
                ),
              ),
            ],
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
          child: FilledButton(
            onPressed: _salvando ? null : _salvar,
            child: _salvando
                ? const SizedBox(
                    height: 22,
                    width: 22,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : Text(_editando ? 'Salvar alterações' : 'Cadastrar produto'),
          ),
        ),
      ),
    );
  }
}

class _Secao extends StatelessWidget {
  const _Secao(this.titulo);

  final String titulo;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 24, bottom: 10),
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

/// Margem e lucro por unidade, recalculados enquanto a pessoa digita.
class _Margem extends StatelessWidget {
  const _Margem({required this.precoCentavos, required this.custoCentavos, required this.unidade});

  final int precoCentavos;
  final int custoCentavos;
  final StockUnit unidade;

  @override
  Widget build(BuildContext context) {
    if (precoCentavos == 0) return const SizedBox.shrink();

    final lucro = precoCentavos - custoCentavos;
    final (texto, cor, icone) = lucro < 0
        ? (
            'Preço abaixo do custo: prejuízo de ${formatarMoeda(-lucro)} por ${unidade.sigla}',
            AppColors.perigo,
            Icons.trending_down_rounded,
          )
        : (
            'Margem de ${formatarPercentual(lucro * 100 / precoCentavos)} · '
                'lucro de ${formatarMoeda(lucro)} por ${unidade.sigla}',
            AppColors.sucesso,
            Icons.trending_up_rounded,
          );

    return Container(
      margin: const EdgeInsets.only(top: 12),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: cor.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          Icon(icone, size: 20, color: cor),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              texto,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: cor),
            ),
          ),
        ],
      ),
    );
  }
}

class _EstoqueAtual extends StatelessWidget {
  const _EstoqueAtual({required this.produto, required this.unidade});

  final Product produto;
  final StockUnit unidade;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.campo,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Estoque atual: ${formatarQuantidade(produto.estoque)} ${unidade.sigla}',
            style: texto.bodyLarge?.copyWith(fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 4),
          Text(
            'O estoque entra pelas compras e sai pelas vendas.',
            style: texto.bodySmall?.copyWith(color: AppColors.textoSecundario),
          ),
        ],
      ),
    );
  }
}
