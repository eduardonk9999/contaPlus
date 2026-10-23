import 'package:flutter/material.dart';

import '../theme.dart';

/// Marca do Conta+: carteira branca num "badge" azul arredondado.
class AppLogo extends StatelessWidget {
  const AppLogo({super.key, this.size = 76});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: AppColors.azulBB,
        borderRadius: BorderRadius.circular(size * 0.29),
      ),
      child: Icon(
        Icons.account_balance_wallet_rounded,
        color: Colors.white,
        size: size / 2,
      ),
    );
  }
}
