package com.contaplus.api.cashregister;

public class CashRegisterAlreadyOpenException extends RuntimeException {
    public CashRegisterAlreadyOpenException(String message) {
        super(message);
    }
}
