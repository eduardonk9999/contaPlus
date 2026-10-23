import 'package:flutter/material.dart';

import 'core/api_client.dart';
import 'home_page.dart';
import 'models/auth_models.dart';
import 'products/product_models.dart';
import 'products/product_service.dart';
import 'products/products_page.dart';
import 'sales/sale_models.dart';
import 'sales/sale_page.dart';
import 'sales/checkout_page.dart';
import 'sales/sale_service.dart';
import 'voice/expense_confirm_page.dart';
import 'voice/purchase_confirm_page.dart';
import 'voice/speech.dart';
import 'voice/voice_models.dart';
import 'voice/voice_service.dart';
import 'voice/voice_sheet.dart';
import 'core/formatters.dart';
import 'theme.dart';

/// Estrutura do app logado: abas com barra de navegação inferior.
class AppShell extends StatefulWidget {
  const AppShell({
    super.key,
    required this.user,
    required this.api,
    this.reconhecedor,
  });

  final UserAccount user;
  final ApiClient api;

  /// Microfone; nos testes entra um falso.
  final Reconhecedor? reconhecedor;

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  static const _inicio = 0;
  static const _vender = 1;
  static const _produtos = 2;

  int _aba = _inicio;

  // Cada aba só é montada (e só chama a API) na primeira vez que é aberta;
  // depois fica viva para manter rolagem, busca e o carrinho ao trocar de aba.
  final _abasAbertas = <int>{_inicio};

  late final _produtoService = ProductService(widget.api);
  late final _vendaService = SaleService(widget.api);
  late final _vozService = VoiceService(widget.api);
  final _carrinho = Cart();

  @override
  void dispose() {
    _carrinho.dispose();
    super.dispose();
  }

  void _irPara(int aba) => setState(() {
    _aba = aba;
    _abasAbertas.add(aba);
  });

  /// Fala (ou texto) -> interpretação -> confirmação do tipo certo. Nada é
  /// gravado sem o lojista confirmar.
  Future<void> _lancarPorVoz() async {
    final interpretacao = await pedirLancamento(
      context,
      servico: _vozService,
      reconhecedor: widget.reconhecedor,
    );
    if (interpretacao == null || !mounted) return;

    switch (interpretacao.tipo) {
      case TipoLancamento.venda:
        await _vendaPorVoz(interpretacao);
      case TipoLancamento.compra:
        final total = await Navigator.of(context).push<int>(
          MaterialPageRoute(
            builder: (_) => PurchaseConfirmPage(
              interpretacao: interpretacao,
              servico: _vozService,
              storeId: widget.user.storeId,
            ),
          ),
        );
        if (total != null) {
          _avisar(
            'Entrada de ${formatarMoeda(total)} registrada. Estoque atualizado.',
          );
        }
      case TipoLancamento.despesa:
        final gravou = await Navigator.of(context).push<bool>(
          MaterialPageRoute(
            builder: (_) => ExpenseConfirmPage(
              interpretacao: interpretacao,
              servico: _vozService,
              storeId: widget.user.storeId,
            ),
          ),
        );
        if (gravou == true) _avisar('Despesa registrada.');
      case TipoLancamento.naoEntendi:
        break;
    }
  }

  /// Monta o carrinho com o que foi dito e vai direto para o Cobrar.
  Future<void> _vendaPorVoz(Interpretacao interpretacao) async {
    if (!_carrinho.vazio && !await _confirmarTrocaDoCarrinho()) return;

    final List<Product> produtos;
    try {
      // Produto completo (preço e estoque atuais) para o carrinho e o preview.
      produtos = await Future.wait(
        interpretacao.itens.map(
          (item) => _produtoService.buscar(item.productId),
        ),
      );
    } on ApiException catch (e) {
      _avisar(e.message, erro: true);
      return;
    }
    if (!mounted) return;

    _carrinho.limpar();
    for (var i = 0; i < produtos.length; i++) {
      _carrinho.adicionar(produtos[i], interpretacao.itens[i].quantidade);
    }
    _carrinho.origem = interpretacao;
    _irPara(_vender);

    if (interpretacao.naoEncontrados.isNotEmpty) {
      _avisar(
        'Não achei no cadastro: ${interpretacao.naoEncontrados.join(', ')}.',
        erro: true,
      );
    }
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => CheckoutPage(
          cart: _carrinho,
          vendas: _vendaService,
          storeId: widget.user.storeId,
          formaInicialId: interpretacao.formaPagamentoId,
          recebidoInicialCentavos: interpretacao.valorPagoCentavos,
        ),
      ),
    );
  }

  Future<bool> _confirmarTrocaDoCarrinho() async {
    final trocar = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Trocar o carrinho?'),
        content: const Text(
          'Já tem uma venda em andamento. A venda falada vai substituir os itens dela.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Manter'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Substituir'),
          ),
        ],
      ),
    );
    return trocar ?? false;
  }

  void _avisar(String mensagem, {bool erro = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          backgroundColor: erro ? AppColors.alerta : AppColors.sucesso,
          content: Text(mensagem),
        ),
      );
  }

  Widget _seAberta(int aba, Widget Function() construir) =>
      _abasAbertas.contains(aba) ? construir() : const SizedBox.shrink();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _aba,
        children: [
          HomePage(user: widget.user, onFalar: _lancarPorVoz),
          _seAberta(
            _vender,
            () => SalePage(
              produtos: _produtoService,
              vendas: _vendaService,
              cart: _carrinho,
              storeId: widget.user.storeId,
              onFalar: _lancarPorVoz,
            ),
          ),
          _seAberta(
            _produtos,
            () => ProductsPage(
              service: _produtoService,
              storeId: widget.user.storeId,
            ),
          ),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _aba,
        onDestinationSelected: _irPara,
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home_rounded),
            label: 'Início',
          ),
          NavigationDestination(
            icon: Icon(Icons.point_of_sale_outlined),
            selectedIcon: Icon(Icons.point_of_sale_rounded),
            label: 'Vender',
          ),
          NavigationDestination(
            icon: Icon(Icons.inventory_2_outlined),
            selectedIcon: Icon(Icons.inventory_2_rounded),
            label: 'Produtos',
          ),
        ],
      ),
    );
  }
}
