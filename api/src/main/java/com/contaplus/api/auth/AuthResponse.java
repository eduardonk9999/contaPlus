package com.contaplus.api.auth;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
