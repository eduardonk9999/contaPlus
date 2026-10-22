import 'package:flutter/material.dart';

import 'home_page.dart';
import 'services/auth_service.dart';
import 'theme.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _emailController = TextEditingController();
  final _senhaController = TextEditingController();
  final _authService = AuthService();

  bool _carregando = false;
  bool _ocultarSenha = true;

  @override
  void dispose() {
    _emailController.dispose();
    _senhaController.dispose();
    super.dispose();
  }

  Future<void> _entrar() async {
    FocusScope.of(context).unfocus(); // fecha o teclado
    setState(() => _carregando = true);
    try {
      final result = await _authService.login(
        _emailController.text.trim(),
        _senhaController.text,
      );
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => HomePage(user: result.user)),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          backgroundColor: Colors.red.shade700,
          content: Text(e.toString()),
        ),
      );
    } finally {
      if (mounted) setState(() => _carregando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 32),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Logo em "badge" azul arredondado.
                  Center(
                    child: Container(
                      width: 76,
                      height: 76,
                      decoration: BoxDecoration(
                        color: AppColors.azulBB,
                        borderRadius: BorderRadius.circular(22),
                      ),
                      child: const Icon(
                        Icons.account_balance_wallet_rounded,
                        color: Colors.white,
                        size: 38,
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                  Text(
                    'Conta+',
                    textAlign: TextAlign.center,
                    style: texto.headlineMedium?.copyWith(
                      color: AppColors.azulBB,
                      fontWeight: FontWeight.w700,
                      letterSpacing: -0.5,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Entre na sua conta',
                    textAlign: TextAlign.center,
                    style: texto.bodyLarge?.copyWith(
                      color: AppColors.textoSecundario,
                    ),
                  ),
                  const SizedBox(height: 40),
                  TextField(
                    controller: _emailController,
                    keyboardType: TextInputType.emailAddress,
                    textInputAction: TextInputAction.next,
                    decoration: const InputDecoration(
                      hintText: 'Email',
                      prefixIcon: Icon(Icons.mail_outline_rounded),
                    ),
                  ),
                  const SizedBox(height: 14),
                  TextField(
                    controller: _senhaController,
                    obscureText: _ocultarSenha,
                    textInputAction: TextInputAction.done,
                    onSubmitted: (_) => _carregando ? null : _entrar(),
                    decoration: InputDecoration(
                      hintText: 'Senha',
                      prefixIcon: const Icon(Icons.lock_outline_rounded),
                      suffixIcon: IconButton(
                        icon: Icon(
                          _ocultarSenha
                              ? Icons.visibility_off_outlined
                              : Icons.visibility_outlined,
                        ),
                        onPressed: () =>
                            setState(() => _ocultarSenha = !_ocultarSenha),
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton(
                      onPressed: _carregando ? null : () {},
                      child: const Text('Esqueci a senha'),
                    ),
                  ),
                  const SizedBox(height: 14),
                  FilledButton(
                    onPressed: _carregando ? null : _entrar,
                    child: _carregando
                        ? const SizedBox(
                            height: 22,
                            width: 22,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Text('Entrar'),
                  ),
                  const SizedBox(height: 24),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        'Não tem conta?',
                        style: texto.bodyMedium?.copyWith(
                          color: AppColors.textoSecundario,
                        ),
                      ),
                      TextButton(
                        onPressed: _carregando ? null : () {},
                        child: const Text('Criar conta'),
                      ),
                    ],
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
