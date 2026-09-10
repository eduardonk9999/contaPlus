CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    product_id UUID NOT NULL REFERENCES products(id),
    transaction_id UUID REFERENCES transactions(id),
    type VARCHAR(20) NOT NULL,
    quantity_delta NUMERIC(15,3) NOT NULL,
    quantity_before NUMERIC(15,3) NOT NULL,
    quantity_after NUMERIC(15,3) NOT NULL,
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movements_product_id ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_store_id ON stock_movements(store_id);
CREATE INDEX idx_stock_movements_transaction_id ON stock_movements(transaction_id);
CREATE INDEX idx_stock_movements_occurred_at ON stock_movements(occurred_at);
