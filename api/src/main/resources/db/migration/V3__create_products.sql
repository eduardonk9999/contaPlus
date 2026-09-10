CREATE TABLE products (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'SELLABLE',
    cost_price_cents INTEGER NOT NULL,
    sale_price_cents INTEGER NOT NULL,
    stock_quantity NUMERIC(15,3) NOT NULL DEFAULT 0,
    stock_unit VARCHAR(20) NOT NULL DEFAULT 'UNIT',
    min_stock_quantity NUMERIC(15,3) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_products_store_name_active UNIQUE (store_id, name)
);

CREATE INDEX idx_products_store_id ON products(store_id);
CREATE INDEX idx_products_active ON products(active);
