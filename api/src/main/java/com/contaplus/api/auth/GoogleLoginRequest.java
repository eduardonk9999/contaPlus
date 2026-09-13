package com.contaplus.api.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "Token do Google é obrigatório")
        String idToken
) {
}
