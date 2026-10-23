import 'package:flutter/material.dart';

import 'models/auth_models.dart';
import 'session/session_controller.dart';

/// Tela provisória pós-login: confirma a sessão mostrando os dados da API.
class HomePage extends StatelessWidget {
  final UserAccount user;
  const HomePage({super.key, required this.user});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Conta+'),
        actions: [
          IconButton(
            tooltip: 'Sair',
            icon: const Icon(Icons.logout),
            onPressed: () => SessionScope.read(context).logout(),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Olá, ${user.name}',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 16),
            _Linha(rotulo: 'Loja', valor: user.storeName),
            _Linha(rotulo: 'Perfil', valor: user.role),
            _Linha(rotulo: 'Email', valor: user.email),
          ],
        ),
      ),
    );
  }
}

class _Linha extends StatelessWidget {
  final String rotulo;
  final String valor;
  const _Linha({required this.rotulo, required this.valor});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Text('$rotulo: $valor'),
    );
  }
}
