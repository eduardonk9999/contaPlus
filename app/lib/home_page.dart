import 'package:flutter/material.dart';

import 'models/auth_models.dart';
import 'session/session_controller.dart';
import 'theme.dart';

/// Início: o atalho principal do app é falar o que aconteceu na loja.
class HomePage extends StatelessWidget {
  const HomePage({super.key, required this.user, required this.onFalar});

  final UserAccount user;
  final VoidCallback onFalar;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;

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
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
        children: [
          Text(
            'Olá, ${user.name.split(' ').first}',
            style: texto.headlineSmall?.copyWith(fontWeight: FontWeight.w700),
          ),
          Text(
            user.storeName,
            style: texto.bodyMedium?.copyWith(color: AppColors.textoSecundario),
          ),
          const SizedBox(height: 20),
          _CartaoFalar(onTap: onFalar),
        ],
      ),
    );
  }
}

class _CartaoFalar extends StatelessWidget {
  const _CartaoFalar({required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;

    return Material(
      color: AppColors.azulBB,
      borderRadius: BorderRadius.circular(20),
      child: InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Row(
            children: [
              Container(
                width: 56,
                height: 56,
                decoration: const BoxDecoration(
                  color: AppColors.branco,
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.mic_rounded,
                  size: 30,
                  color: AppColors.azulBB,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Toque e fale',
                      style: texto.titleLarge?.copyWith(
                        color: AppColors.branco,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Venda, entrada de mercadoria ou conta paga: eu registro pra você.',
                      style: texto.bodyMedium?.copyWith(
                        color: AppColors.branco.withValues(alpha: 0.9),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
