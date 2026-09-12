# TODO - Funcionalidades Pendentes

## Prioridade Alta

### Autenticação e Segurança
- [ ] Implementar Spring Security
- [ ] Autenticação JWT
- [ ] Módulo de usuários (User entity, roles)
- [ ] Associar usuário à loja
- [ ] Proteção de endpoints

### Módulo de Compras
- [ ] PurchaseController e PurchaseService
- [ ] Endpoint POST `/v1/purchases` para registrar compras
- [ ] Atualizar estoque automaticamente (incrementar)
- [ ] Criar StockMovement tipo PURCHASE

### Cancelamento de Vendas
- [ ] Endpoint PUT `/v1/sales/{id}/cancel`
- [ ] Reverter estoque ao cancelar
- [ ] Criar StockMovement tipo CANCELLATION

## Prioridade Média

### Clientes
- [ ] Entidade Customer
- [ ] CRUD de clientes
- [ ] Associar cliente à venda (opcional)
- [ ] Histórico de compras por cliente

### Formas de Pagamento
- [ ] Entidade PaymentMethod
- [ ] Associar pagamento à transação
- [ ] Suportar múltiplas formas na mesma venda
- [ ] Controle de troco

### Caixa
- [ ] Entidade CashRegister
- [ ] Abertura/fechamento de caixa
- [ ] Sangria e suprimento
- [ ] Relatório de fechamento

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