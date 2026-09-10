CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    description TEXT NOT NULL,
    total_amount_cents INTEGER NOT NULL,
    total_cost_cents INTEGER NOT NULL,
    gross_profit_cents INTEGER NOT NULL,
    margin_percent NUMERIC(5,2),
    original_input TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    idempotency_key UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_transactions_store_idempotency UNIQUE (store_id, idempotency_key)
);

CREATE INDEX idx_transactions_store_id ON transactions(store_id);
CREATE INDEX idx_transactions_occurred_at ON transactions(occurred_at);
CREATE INDEX idx_transactions_type ON transactions(type);
