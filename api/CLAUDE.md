# CLAUDE.md - Instruções para o Assistente

## Contexto do Projeto

**ContaPlus** é uma API REST para gestão de vendas, estoque e finanças de pequenos comércios.

## Stack Tecnológica

- Java 21
- Spring Boot 4.1.0
- PostgreSQL
- Flyway (migrations)
- Testcontainers (testes integrados)
- Maven

## Estrutura do Projeto

```
src/main/java/com/contaplus/api/
├── exception/       # GlobalExceptionHandler, ErrorResponse
├── health/          # HealthController
├── product/         # Product (Entity, Controller, Service, Repository)
├── sale/            # SaleService, SaleController, SalePreviewService
├── stock/           # StockMovement, StockService
├── store/           # Store (Entity, Controller, Service, Repository)
├── transaction/     # Transaction, TransactionItem, enums
└── ContaPlusApplication.java
```

## Padrões Adotados

1. **Arquitetura em camadas**: Controller → Service → Repository → Entity
2. **Preços em centavos**: Valores monetários são `Integer` (evita float)
3. **Quantidades com precisão**: `BigDecimal(15,3)` para estoque
4. **UUID para IDs**: Gerados automaticamente
5. **Idempotência**: `idempotencyKey` em transações
6. **Snapshots**: TransactionItem captura nome/unidade do produto no momento da venda
7. **Soft delete**: Produtos são marcados como `active=false`

## Banco de Dados

- **Host**: localhost:5432
- **Database**: contaplus
- **User/Pass**: contaplus/contaplus
- **Migrations**: `src/main/resources/db/migration/`

## Comandos Úteis

```bash
# Rodar aplicação
./mvnw spring-boot:run

# Rodar testes
./mvnw test

# Build
./mvnw clean package
```

## Regras de Negócio Importantes

1. **Produto único por loja**: Não pode ter dois produtos com mesmo nome na mesma store
2. **Venda atualiza estoque**: Ao confirmar venda, estoque é decrementado automaticamente
3. **Preview não persiste**: `/v1/sales/preview` simula sem salvar
4. **Margem calculada**: `grossProfitCents = totalAmountCents - totalCostCents`

## Status Atual

A API está funcional com os módulos básicos implementados. Consulte `TODO.md` para funcionalidades pendentes.

## Convenções de Código

- Records para DTOs
- `@Transactional` em métodos que alteram dados
- Validação com Jakarta Validation (`@NotBlank`, `@NotNull`, `@Min`)
- Exceções customizadas (`ResourceNotFoundException`)
- Resposta de erro padronizada via `GlobalExceptionHandler`

## Testes

Os testes usam Testcontainers com PostgreSQL. Rodam automaticamente um container Docker.

Arquivos de teste em: `src/test/java/com/contaplus/api/`