-- Nomes em português: o padrão do banco ordena por bytes, então "Água" ficava
-- depois de "Zebra". A colação ICU ordena alfabeticamente, com acentos no lugar certo.
ALTER TABLE products   ALTER COLUMN name TYPE VARCHAR(255) COLLATE "und-x-icu";
ALTER TABLE customers  ALTER COLUMN name TYPE VARCHAR(255) COLLATE "und-x-icu";
ALTER TABLE suppliers  ALTER COLUMN name TYPE VARCHAR(255) COLLATE "und-x-icu";
ALTER TABLE categories ALTER COLUMN name TYPE VARCHAR(100) COLLATE "und-x-icu";

-- Busca que ignora acentos ("agua" encontra "Água"); usada pelo SpecificationBuilder.like.
CREATE EXTENSION IF NOT EXISTS unaccent;

-- O nome de produto só precisa ser único entre os ativos, sem diferenciar maiúsculas
-- (a mesma regra do ProductService). A constraint antiga valia também para os
-- desativados e fazia o recadastro de um nome desativado estourar no banco.
ALTER TABLE products DROP CONSTRAINT uq_products_store_name_active;
CREATE UNIQUE INDEX uq_products_store_name_active
    ON products (store_id, lower(name))
    WHERE active = true;
