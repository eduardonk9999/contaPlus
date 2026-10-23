import 'package:flutter/widgets.dart';

import '../core/api_client.dart';
import '../core/token_store.dart';
import '../models/auth_models.dart';
import '../services/auth_service.dart';

enum SessionStatus { restoring, authenticated, unauthenticated, unavailable }

/// Estado de login do app. O AuthGate troca de tela conforme ele muda.
class SessionController extends ChangeNotifier {
  SessionController({
    required this.api,
    required this.auth,
    required this._tokenStore,
  }) {
    api.onUnauthorized = _onUnauthorized;
  }

  factory SessionController.create() {
    final api = ApiClient();
    return SessionController(
      api: api,
      auth: AuthService(api),
      tokenStore: const SecureTokenStore(),
    );
  }

  final ApiClient api;
  final AuthService auth;
  final TokenStore _tokenStore;

  SessionStatus _status = SessionStatus.restoring;
  UserAccount? _user;
  String? _errorMessage;

  SessionStatus get status => _status;

  UserAccount? get user => _user;

  /// Motivo de [SessionStatus.unavailable], pronto para exibir.
  String? get errorMessage => _errorMessage;

  /// Na abertura do app: reaproveita o token salvo se a API ainda o aceitar.
  Future<void> restore() async {
    _setStatus(SessionStatus.restoring);
    final token = await _tokenStore.read();
    if (token == null) {
      _setStatus(SessionStatus.unauthenticated);
      return;
    }

    api.token = token;
    try {
      _user = await auth.me();
      _setStatus(SessionStatus.authenticated);
    } on UnauthorizedException {
      await _endSession();
    } on ApiException catch (e) {
      // Sem servidor não dá pra saber se o token ainda vale: mantém e deixa tentar de novo.
      _errorMessage = e.message;
      _setStatus(SessionStatus.unavailable);
    }
  }

  Future<void> login(String email, String password) async {
    api.token = null;
    final result = await auth.login(email, password);
    await _tokenStore.write(result.token);
    api.token = result.token;
    _user = result.user;
    _setStatus(SessionStatus.authenticated);
  }

  Future<void> logout() => _endSession();

  void _onUnauthorized() {
    if (_status == SessionStatus.authenticated) {
      _endSession();
    }
  }

  Future<void> _endSession() async {
    api.token = null;
    _user = null;
    _errorMessage = null;
    _setStatus(SessionStatus.unauthenticated);
    await _tokenStore.clear();
  }

  void _setStatus(SessionStatus status) {
    if (_status == status) return;
    _status = status;
    notifyListeners();
  }
}

/// Disponibiliza a sessão para toda a árvore de widgets.
class SessionScope extends InheritedNotifier<SessionController> {
  const SessionScope({super.key, required SessionController session, required super.child})
      : super(notifier: session);

  /// Para o build: o widget é reconstruído quando a sessão muda.
  static SessionController of(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<SessionScope>()!.notifier!;

  /// Para callbacks (onPressed etc.), sem criar dependência.
  static SessionController read(BuildContext context) =>
      context.getInheritedWidgetOfExactType<SessionScope>()!.notifier!;
}
