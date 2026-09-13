package com.contaplus.api.cashregister;

public class NoCashRegisterOpenException extends RuntimeException {
    public NoCashRegisterOpenException(String message) {
        super(message);
    }
}
