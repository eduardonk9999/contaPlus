import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Onde o JWT da sessão fica guardado entre aberturas do app.
abstract class TokenStore {
  Future<String?> read();

  Future<void> write(String token);

  Future<void> clear();
}

/// Keystore (Android) / Keychain (iOS).
class SecureTokenStore implements TokenStore {
  const SecureTokenStore([this._storage = const FlutterSecureStorage()]);

  static const _key = 'auth_token';

  final FlutterSecureStorage _storage;

  @override
  Future<String?> read() async {
    try {
      return await _storage.read(key: _key);
    } catch (_) {
      // Valor ilegível (ex.: chave do Keystore perdida): trata como deslogado.
      await clear();
      return null;
    }
  }

  @override
  Future<void> write(String token) => _storage.write(key: _key, value: token);

  @override
  Future<void> clear() async {
    try {
      await _storage.delete(key: _key);
    } catch (_) {
      // Nada a fazer: o pior caso é o token ilegível ser descartado na próxima leitura.
    }
  }
}
