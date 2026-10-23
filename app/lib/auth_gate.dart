import 'package:flutter/material.dart';

import 'home_page.dart';
import 'login_page.dart';
import 'session/session_controller.dart';
import 'theme.dart';
import 'widgets/app_logo.dart';

/// Tela raiz: mostra login, home ou espera, conforme o estado da sessão.
class AuthGate extends StatelessWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context) {
    final session = SessionScope.of(context);
    return switch (session.status) {
      SessionStatus.restoring => const _SplashView(),
      SessionStatus.authenticated => HomePage(user: session.user!),
      SessionStatus.unauthenticated => const LoginPage(),
      SessionStatus.unavailable => _UnavailableView(
          message: session.errorMessage ?? 'Não foi possível conectar ao servidor.',
        ),
    };
  }
}

class _SplashView extends StatelessWidget {
  const _SplashView();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AppLogo(),
            SizedBox(height: 28),
            SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(strokeWidth: 2.5),
            ),
          ],
        ),
      ),
    );
  }
}

class _UnavailableView extends StatelessWidget {
  const _UnavailableView({required this.message});

  final String message;

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
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Icon(Icons.cloud_off_rounded, size: 56, color: AppColors.textoSuave),
                  const SizedBox(height: 16),
                  Text(
                    'Sem conexão com o servidor',
                    textAlign: TextAlign.center,
                    style: texto.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    message,
                    textAlign: TextAlign.center,
                    style: texto.bodyMedium?.copyWith(color: AppColors.textoSecundario),
                  ),
                  const SizedBox(height: 28),
                  FilledButton(
                    onPressed: () => SessionScope.read(context).restore(),
                    child: const Text('Tentar de novo'),
                  ),
                  const SizedBox(height: 8),
                  TextButton(
                    onPressed: () => SessionScope.read(context).logout(),
                    child: const Text('Sair da conta'),
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
