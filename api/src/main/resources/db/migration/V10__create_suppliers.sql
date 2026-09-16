CREATE TABLE suppliers (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    name VARCHAR(255) NOT NULL,
    cnpj VARCHAR(18),
    phone VARCHAR(20),
    email VARCHAR(255),
    address TEXT,
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_suppliers_store_id ON suppliers(store_id);
CREATE INDEX idx_suppliers_cnpj ON suppliers(cnpj);

ALTER TABLE transactions ADD COLUMN supplier_id UUID REFERENCES suppliers(id);
CREATE INDEX idx_transactions_supplier_id ON transactions(supplier_id);
