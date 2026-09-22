-- Formas de pagamento disponíveis na loja
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL, -- CASH, CREDIT_CARD, DEBIT_CARD, PIX, TRANSFER, OTHER
    active BOOLEAN NOT NULL DEFAULT true,
    accepts_change BOOLEAN NOT NULL DEFAULT false, -- Se aceita troco (ex: dinheiro)
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payment_methods_store_id ON payment_methods(store_id);
CREATE UNIQUE INDEX idx_payment_methods_store_name ON payment_methods(store_id, name) WHERE active = true;

-- Pagamentos realizados em uma transação
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    payment_method_id UUID NOT NULL REFERENCES payment_methods(id),
    amount_cents INTEGER NOT NULL, -- Valor pago nesta forma
    change_cents INTEGER NOT NULL DEFAULT 0, -- Troco (apenas para dinheiro)
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_payment_method_id ON payments(payment_method_id);
