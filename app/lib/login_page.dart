import 'package:flutter/material.dart';

import 'core/api_client.dart';
import 'session/session_controller.dart';
import 'theme.dart';
import 'widgets/app_logo.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _emailController = TextEditingController();
  final _senhaController = TextEditingController();

  bool _carregando = false;
  bool _ocultarSenha = true;

  @override
  void dispose() {
    _emailController.dispose();
    _senhaController.dispose();
    super.dispose();
  }

  Future<void> _entrar() async {
    final email = _emailController.text.trim();
    final senha = _senhaController.text;
    if (email.isEmpty || senha.isEmpty) {
      _mostrarErro('Preencha email e senha.');
      return;
    }

    final session = SessionScope.read(context);
    FocusScope.of(context).unfocus();
    setState(() => _carregando = true);
    try {
      // No sucesso a tela troca sozinha: o AuthGate reage à sessão.
      await session.login(email, senha);
    } on ApiException catch (e) {
      _mostrarErro(e.message);
    } catch (_) {
      _mostrarErro('Algo deu errado. Tente de novo.');
    } finally {
      if (mounted) setState(() => _carregando = false);
    }
  }

  void _mostrarErro(String mensagem) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          backgroundColor: Colors.red.shade700,
          content: Text(mensagem),
        ),
      );
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
                  const Center(child: AppLogo()),
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
                    autocorrect: false,
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
