package com.contaplus.api.security;

public class UnauthorizedStoreAccessException extends RuntimeException {

    public UnauthorizedStoreAccessException(String message) {
        super(message);
    }
}
