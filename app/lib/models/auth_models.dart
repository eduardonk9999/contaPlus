/// Espelha o UserResponse da API (campos UUID chegam como String no JSON).
class UserAccount {
  final String id;
  final String email;
  final String name;
  final String? cnpj;
  final String role;
  final String storeId;
  final String storeName;

  UserAccount({
    required this.id,
    required this.email,
    required this.name,
    required this.cnpj,
    required this.role,
    required this.storeId,
    required this.storeName,
  });

  factory UserAccount.fromJson(Map<String, dynamic> json) {
    return UserAccount(
      id: json['id'] as String,
      email: json['email'] as String,
      name: json['name'] as String,
      cnpj: json['cnpj'] as String?,
      role: json['role'] as String,
      storeId: json['storeId'] as String,
      storeName: json['storeName'] as String,
    );
  }
}

/// Espelha o AuthResponse da API: { token, user }.
class AuthResult {
  final String token;
  final UserAccount user;

  AuthResult({required this.token, required this.user});

  factory AuthResult.fromJson(Map<String, dynamic> json) {
    return AuthResult(
      token: json['token'] as String,
      user: UserAccount.fromJson(json['user'] as Map<String, dynamic>),
    );
  }
}
