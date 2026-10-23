-- O cadastro de loja não criava as formas de pagamento padrão (o método existia mas
-- nunca era chamado), então lojas antigas ficaram sem nenhuma e não conseguiam cobrar.
-- Completa só as lojas que não têm nenhuma forma de pagamento.
INSERT INTO payment_methods (id, store_id, name, type, active, accepts_change, created_at, updated_at)
SELECT gen_random_uuid(), s.id, padrao.name, padrao.type, true, padrao.accepts_change, now(), now()
FROM stores s
CROSS JOIN (VALUES
    ('Dinheiro', 'CASH', true),
    ('PIX', 'PIX', false),
    ('Cartão de Crédito', 'CREDIT_CARD', false),
    ('Cartão de Débito', 'DEBIT_CARD', false)
) AS padrao(name, type, accepts_change)
WHERE NOT EXISTS (SELECT 1 FROM payment_methods pm WHERE pm.store_id = s.id);
