CREATE TABLE cash_registers (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    opened_by UUID NOT NULL REFERENCES users(id),
    closed_by UUID REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    opening_amount_cents INTEGER NOT NULL,
    closing_amount_cents INTEGER,
    expected_amount_cents INTEGER,
    difference_cents INTEGER,
    notes TEXT,
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_cash_registers_store_id ON cash_registers(store_id);
CREATE INDEX idx_cash_registers_status ON cash_registers(status);
CREATE INDEX idx_cash_registers_opened_at ON cash_registers(opened_at);

CREATE TABLE cash_movements (
    id UUID PRIMARY KEY,
    cash_register_id UUID NOT NULL REFERENCES cash_registers(id),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(20) NOT NULL,
    amount_cents INTEGER NOT NULL,
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_cash_movements_cash_register_id ON cash_movements(cash_register_id);
CREATE INDEX idx_cash_movements_type ON cash_movements(type);
