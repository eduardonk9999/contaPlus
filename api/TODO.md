# TODO - Funcionalidades Pendentes

## Prioridade Alta

### ~~Autenticação e Segurança~~ ✅
- [x] Implementar Spring Security
- [x] Autenticação JWT
- [x] Módulo de usuários (User entity, roles)
- [x] Associar usuário à loja
- [x] Proteção de endpoints
- [x] Login com Google (OAuth2)

### ~~Módulo de Compras~~ ✅
- [x] PurchaseController e PurchaseService
- [x] Endpoint POST `/v1/purchases` para registrar compras
- [x] Atualizar estoque automaticamente (incrementar)
- [x] Criar StockMovement tipo PURCHASE
- [x] Atualizar custo do produto na compra (opcional)

### ~~Cancelamento de Vendas~~ ✅
- [x] Endpoint PUT `/v1/sales/{id}/cancel`
- [x] Reverter estoque ao cancelar
- [x] Criar StockMovement tipo REVERSAL

## Prioridade Média

### ~~Clientes~~ ✅
- [x] Entidade Customer
- [x] CRUD de clientes
- [x] Associar cliente à venda (opcional)
- [x] Histórico de compras por cliente

### ~~Formas de Pagamento~~ ✅
- [x] Entidade PaymentMethod
- [x] Associar pagamento à transação
- [x] Suportar múltiplas formas na mesma venda
- [x] Controle de troco

### ~~Caixa~~ ✅
- [x] Entidade CashRegister
- [x] Abertura/fechamento de caixa
- [x] Sangria e suprimento
- [x] Relatório de fechamento

### ~~Relatórios~~ ✅
- [x] Vendas por período
- [x] Produtos mais vendidos
- [x] Margem de lucro por produto
- [x] Estoque baixo (alerta)
- [x] Movimentações de estoque
- [x] Dashboard consolidado

## Prioridade Baixa

### ~~Documentação~~ ✅
- [x] Swagger/OpenAPI
- [x] Documentar DTOs de request/response

### Melhorias Técnicas
- [x] Paginação nos endpoints de listagem
- [x] Filtros avançados (data, status, etc)
- [x] Cache com Redis
- [x] Rate limiting
- [x] Logs estruturados (JSON)

### ~~Fornecedores~~ ✅
- [x] Entidade Supplier
- [x] CRUD de fornecedores
- [x] Associar fornecedor à compra

### ~~Categorias~~ ✅
- [x] Entidade Category
- [x] Categorizar produtos
- [x] Relatórios por categoria

## Concluído

- [x] CRUD de lojas (Store)
- [x] CRUD de produtos (Product)
- [x] Vendas com preview
- [x] Controle de estoque automático
- [x] Movimentações de estoque
- [x] Transações com itens
- [x] Cálculo de margem e lucro
- [x] Idempotência em vendas
- [x] Testes integrados
- [x] Migrations Flyway
- [x] Tratamento global de exceções