import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/auth_models.dart';

/// Erro de autenticação com mensagem amigável pra mostrar na tela.
class AuthException implements Exception {
  final String message;
  AuthException(this.message);
  @override
  String toString() => message;
}

/// Fala com os endpoints /v1/auth da API Conta+.
class AuthService {
  /// No emulador Android, 10.0.2.2 é o atalho para o localhost do host (seu Mac).
  /// Porta 8082 porque a 8081 está ocupada pelo container WordPress (ahfip).
  /// (No simulador iOS seria http://localhost:8082.)
  static const String baseUrl = 'http://10.0.2.2:8082';

  Future<AuthResult> login(String email, String password) async {
    final uri = Uri.parse('$baseUrl/v1/auth/login');

    late final http.Response resp;
    try {
      resp = await http
          .post(
            uri,
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'email': email, 'password': password}),
          )
          .timeout(const Duration(seconds: 10));
    } on TimeoutException {
      throw AuthException('Servidor demorou a responder. A API está no ar?');
    } catch (_) {
      throw AuthException('Não foi possível conectar à API (porta 8081).');
    }

    if (resp.statusCode == 200) {
      final json = jsonDecode(resp.body) as Map<String, dynamic>;
      return AuthResult.fromJson(json);
    }
    if (resp.statusCode == 401) {
      throw AuthException('Email ou senha inválidos.');
    }
    throw AuthException('Falha no login (HTTP ${resp.statusCode}).');
  }
}
