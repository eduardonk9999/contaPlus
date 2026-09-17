CREATE TABLE categories (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    color VARCHAR(7),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(store_id, name)
);

CREATE INDEX idx_categories_store_id ON categories(store_id);

ALTER TABLE products ADD COLUMN category_id UUID REFERENCES categories(id);
CREATE INDEX idx_products_category_id ON products(category_id);
