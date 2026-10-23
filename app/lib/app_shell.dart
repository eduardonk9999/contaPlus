import 'package:flutter/material.dart';

import 'core/api_client.dart';
import 'home_page.dart';
import 'models/auth_models.dart';
import 'products/product_service.dart';
import 'products/products_page.dart';

/// Estrutura do app logado: abas com barra de navegação inferior.
class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.user, required this.api});

  final UserAccount user;
  final ApiClient api;

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  int _aba = 0;

  // Cada aba só é montada (e só chama a API) na primeira vez que é aberta;
  // depois fica viva para manter rolagem e busca ao trocar de aba.
  final _abasAbertas = <int>{0};

  late final _produtos = ProductService(widget.api);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _aba,
        children: [
          HomePage(user: widget.user),
          if (_abasAbertas.contains(1))
            ProductsPage(service: _produtos, storeId: widget.user.storeId)
          else
            const SizedBox.shrink(),
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
            icon: Icon(Icons.inventory_2_outlined),
            selectedIcon: Icon(Icons.inventory_2_rounded),
            label: 'Produtos',
          ),
        ],
      ),
    );
  }
}
