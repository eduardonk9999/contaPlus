package com.contaplus.api.cashregister;

public class CashRegisterClosedException extends RuntimeException {
    public CashRegisterClosedException(String message) {
        super(message);
    }
}
