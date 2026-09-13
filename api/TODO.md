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

### Formas de Pagamento
- [ ] Entidade PaymentMethod
- [ ] Associar pagamento à transação
- [ ] Suportar múltiplas formas na mesma venda
- [ ] Controle de troco

### ~~Caixa~~ ✅
- [x] Entidade CashRegister
- [x] Abertura/fechamento de caixa
- [x] Sangria e suprimento
- [x] Relatório de fechamento

### Relatórios
- [ ] Vendas por período
- [ ] Produtos mais vendidos
- [ ] Margem de lucro por produto
- [ ] Estoque baixo (alerta)
- [ ] Movimentações de estoque

## Prioridade Baixa

### Documentação
- [ ] Swagger/OpenAPI
- [ ] Documentar DTOs de request/response

### Melhorias Técnicas
- [ ] Paginação nos endpoints de listagem
- [ ] Filtros avançados (data, status, etc)
- [ ] Cache com Redis
- [ ] Rate limiting
- [ ] Logs estruturados (JSON)

### Fornecedores
- [ ] Entidade Supplier
- [ ] CRUD de fornecedores
- [ ] Associar fornecedor à compra

### Categorias
- [ ] Entidade Category
- [ ] Categorizar produtos
- [ ] Relatórios por categoria

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