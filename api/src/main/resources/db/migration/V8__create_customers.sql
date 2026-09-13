CREATE TABLE customers (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(255),
    cpf_cnpj VARCHAR(18),
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_customers_store_id ON customers(store_id);
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_cpf_cnpj ON customers(cpf_cnpj);

ALTER TABLE transactions ADD COLUMN customer_id UUID REFERENCES customers(id);
CREATE INDEX idx_transactions_customer_id ON transactions(customer_id);
