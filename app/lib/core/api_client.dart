import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

/// Erro de chamada à API, com mensagem pronta para mostrar ao usuário.
class ApiException implements Exception {
  ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}

/// A API recusou o token (expirado, inválido ou usuário desativado).
class UnauthorizedException extends ApiException {
  UnauthorizedException(super.message, {super.statusCode});
}

/// Nenhuma resposta do servidor: sem rede, API fora do ar ou timeout.
class NetworkException extends ApiException {
  NetworkException(super.message);
}

/// Cliente HTTP da API Conta+: base URL, JSON, token e mapeamento de erros.
class ApiClient {
  ApiClient({http.Client? httpClient, this.baseUrl = defaultBaseUrl})
      : _http = httpClient ?? http.Client();

  /// No emulador Android, 10.0.2.2 é o atalho para o localhost do host (seu Mac).
  /// Porta 8082 porque a 8081 está ocupada pelo container WordPress (ahfip).
  static const defaultBaseUrl = 'http://10.0.2.2:8082';
  static const _timeout = Duration(seconds: 10);

  final http.Client _http;
  final String baseUrl;

  /// JWT da sessão atual; quando presente, vai em toda requisição.
  String? token;

  /// Chamado quando a API rejeita o token de uma requisição autenticada.
  void Function()? onUnauthorized;

  Future<dynamic> get(String path) => _send('GET', path);

  Future<dynamic> post(String path, Object? body) => _send('POST', path, body: body);

  Future<dynamic> put(String path, Object? body) => _send('PUT', path, body: body);

  Future<dynamic> delete(String path) => _send('DELETE', path);

  Future<dynamic> _send(String method, String path, {Object? body}) async {
    final request = http.Request(method, Uri.parse('$baseUrl$path'))
      ..headers['Accept'] = 'application/json';
    if (body != null) {
      request.headers['Content-Type'] = 'application/json';
      request.body = jsonEncode(body);
    }
    final sentToken = token;
    if (sentToken != null) {
      request.headers['Authorization'] = 'Bearer $sentToken';
    }

    final http.Response response;
    try {
      response = await _http.send(request).then(http.Response.fromStream).timeout(_timeout);
    } on TimeoutException {
      throw NetworkException('O servidor demorou a responder. Tente de novo.');
    } on Exception {
      throw NetworkException('Não foi possível conectar ao servidor.');
    }

    final status = response.statusCode;
    final decoded = _decode(response);
    if (status >= 200 && status < 300) {
      return decoded;
    }

    if (status == 401 && sentToken != null) {
      onUnauthorized?.call();
      throw UnauthorizedException('Sua sessão expirou. Entre novamente.', statusCode: status);
    }
    throw ApiException(_errorMessage(decoded) ?? 'Erro inesperado (HTTP $status).', statusCode: status);
  }

  // A API nem sempre manda charset no Content-Type; sem isso, acentos viram lixo.
  dynamic _decode(http.Response response) {
    if (response.bodyBytes.isEmpty) return null;
    try {
      return jsonDecode(utf8.decode(response.bodyBytes));
    } on FormatException {
      return null;
    }
  }

  String? _errorMessage(dynamic body) {
    if (body is Map && body['message'] is String) return body['message'] as String;
    return null;
  }
}
