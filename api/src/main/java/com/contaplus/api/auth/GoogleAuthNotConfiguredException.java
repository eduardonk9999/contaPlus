package com.contaplus.api.auth;

public class GoogleAuthNotConfiguredException extends RuntimeException {
    public GoogleAuthNotConfiguredException(String message) {
        super(message);
    }
}
