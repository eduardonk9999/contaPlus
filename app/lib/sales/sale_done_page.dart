import 'package:flutter/material.dart';

import '../core/formatters.dart';
import '../theme.dart';

class SaleDonePage extends StatelessWidget {
  const SaleDonePage({
    super.key,
    required this.totalCentavos,
    required this.trocoCentavos,
    required this.formaPagamento,
  });

  final int totalCentavos;
  final int trocoCentavos;
  final String formaPagamento;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(28),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 88,
                    height: 88,
                    decoration: BoxDecoration(
                      color: AppColors.sucesso.withValues(alpha: 0.12),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.check_rounded, size: 48, color: AppColors.sucesso),
                  ),
                  const SizedBox(height: 20),
                  Text(
                    'Venda registrada',
                    style: texto.headlineSmall?.copyWith(fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    formatarMoeda(totalCentavos),
                    style: texto.headlineMedium?.copyWith(
                      color: AppColors.azulBB,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  Text(
                    'Pago com $formaPagamento',
                    style: texto.bodyMedium?.copyWith(color: AppColors.textoSecundario),
                  ),
                  if (trocoCentavos > 0) ...[
                    const SizedBox(height: 24),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: AppColors.azulBB.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: Column(
                        children: [
                          Text('Troco a devolver', style: texto.titleMedium),
                          const SizedBox(height: 4),
                          Text(
                            formatarMoeda(trocoCentavos),
                            style: texto.headlineMedium?.copyWith(fontWeight: FontWeight.w700),
                          ),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: 36),
                  FilledButton.icon(
                    onPressed: () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.add_shopping_cart_rounded),
                    label: const Text('Nova venda'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
