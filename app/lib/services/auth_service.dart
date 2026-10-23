import '../core/api_client.dart';
import '../models/auth_models.dart';

/// Endpoints /v1/auth da API Conta+.
class AuthService {
  AuthService(this._api);

  final ApiClient _api;

  Future<AuthResult> login(String email, String password) async {
    try {
      final json = await _api.post('/v1/auth/login', {'email': email, 'password': password});
      return AuthResult.fromJson(json as Map<String, dynamic>);
    } on NetworkException {
      rethrow;
    } on ApiException catch (e) {
      throw switch (e.statusCode) {
        401 => ApiException('Email ou senha inválidos.', statusCode: 401),
        400 => ApiException('Confira o email e a senha informados.', statusCode: 400),
        _ => e,
      };
    }
  }

  Future<UserAccount> me() async {
    final json = await _api.get('/v1/auth/me');
    return UserAccount.fromJson(json as Map<String, dynamic>);
  }
}
