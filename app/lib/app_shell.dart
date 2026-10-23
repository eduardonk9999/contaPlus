import 'package:flutter/material.dart';

import 'core/api_client.dart';
import 'home_page.dart';
import 'models/auth_models.dart';
import 'products/product_service.dart';
import 'products/products_page.dart';
import 'sales/sale_models.dart';
import 'sales/sale_page.dart';
import 'sales/sale_service.dart';

/// Estrutura do app logado: abas com barra de navegação inferior.
class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.user, required this.api});

  final UserAccount user;
  final ApiClient api;

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
  final _carrinho = Cart();

  @override
  void dispose() {
    _carrinho.dispose();
    super.dispose();
  }

  Widget _seAberta(int aba, Widget Function() construir) =>
      _abasAbertas.contains(aba) ? construir() : const SizedBox.shrink();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _aba,
        children: [
          HomePage(user: widget.user),
          _seAberta(
            _vender,
            () => SalePage(
              produtos: _produtoService,
              vendas: _vendaService,
              cart: _carrinho,
              storeId: widget.user.storeId,
            ),
          ),
          _seAberta(
            _produtos,
            () => ProductsPage(service: _produtoService, storeId: widget.user.storeId),
          ),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _aba,
        onDestinationSelected: (aba) => setState(() {
          _aba = aba;
          _abasAbertas.add(aba);
        }),
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
