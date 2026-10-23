-- Contas e despesas da loja que não são compra de mercadoria (luz, aluguel, salários...).
CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    description VARCHAR(255) NOT NULL,
    amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
    category VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    original_input TEXT,
    idempotency_key UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_expenses_store_idempotency UNIQUE (store_id, idempotency_key)
);

CREATE INDEX idx_expenses_store_occurred_at ON expenses(store_id, occurred_at);
