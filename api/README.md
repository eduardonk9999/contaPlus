# ContaPlus API

API REST para gestão de vendas, estoque e finanças de pequenos comércios.

## Tecnologias

- Java 21
- Spring Boot 4.1.0
- PostgreSQL
- Flyway
- Maven

## Requisitos

- Java 21+
- PostgreSQL 15+
- Docker (para testes)

## Configuração

### Banco de Dados

```sql
CREATE DATABASE contaplus;
CREATE USER contaplus WITH PASSWORD 'contaplus';
GRANT ALL PRIVILEGES ON DATABASE contaplus TO contaplus;
```

### Variáveis de Ambiente (opcional)

```bash
export DB_URL=jdbc:postgresql://localhost:5432/contaplus
export DB_USER=contaplus
export DB_PASS=contaplus
```

## Executando

```bash
# Desenvolvimento
./mvnw spring-boot:run

# Build
./mvnw clean package

# Executar JAR
java -jar target/contaPlus-0.0.1-SNAPSHOT.jar
```

A API estará disponível em `http://localhost:8081`

## Testes

```bash
./mvnw test
```

> Requer Docker para Testcontainers

## Endpoints

### Health
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/v1/health` | Status da API |

### Stores (Lojas)
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/v1/stores` | Criar loja |
| GET | `/v1/stores/{id}` | Buscar por ID |
| GET | `/v1/stores` | Listar todas |
| PUT | `/v1/stores/{id}` | Atualizar |
| DELETE | `/v1/stores/{id}` | Deletar |

### Products (Produtos)
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/v1/products` | Criar produto |
| GET | `/v1/products/{id}` | Buscar por ID |
| GET | `/v1/products?storeId=X` | Listar por loja |
| PUT | `/v1/products/{id}` | Atualizar |
| DELETE | `/v1/products/{id}` | Desativar |

### Sales (Vendas)
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/v1/sales` | Confirmar venda |
| POST | `/v1/sales/preview` | Preview (sem persistir) |
| GET | `/v1/sales/{id}` | Buscar por ID |
| GET | `/v1/sales?storeId=X` | Listar por loja |

### Transactions (Transações)
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/v1/transactions/{id}` | Buscar por ID |
| GET | `/v1/transactions?storeId=X` | Listar por loja |

## Exemplos

### Criar Loja
```bash
curl -X POST http://localhost:8081/v1/stores \
  -H "Content-Type: application/json" \
  -d '{"name": "Minha Loja"}'
```

### Criar Produto
```bash
curl -X POST http://localhost:8081/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "storeId": "uuid-da-loja",
    "name": "Produto X",
    "type": "SELLABLE",
    "costPriceCents": 1000,
    "salePriceCents": 1500,
    "stockQuantity": 100,
    "stockUnit": "UNIT"
  }'
```

### Confirmar Venda
```bash
curl -X POST http://localhost:8081/v1/sales \
  -H "Content-Type: application/json" \
  -d '{
    "storeId": "uuid-da-loja",
    "idempotencyKey": "uuid-unico",
    "items": [
      {"productId": "uuid-do-produto", "quantity": 2}
    ]
  }'
```

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/contaplus/api/
│   │   ├── exception/    # Tratamento de erros
│   │   ├── health/       # Health check
│   │   ├── product/      # Módulo de produtos
│   │   ├── sale/         # Módulo de vendas
│   │   ├── stock/        # Controle de estoque
│   │   ├── store/        # Módulo de lojas
│   │   └── transaction/  # Transações
│   └── resources/
│       ├── application.yaml
│       └── db/migration/  # Flyway migrations
└── test/
    └── java/com/contaplus/api/
```

## Licença

Proprietário - Todos os direitos reservados