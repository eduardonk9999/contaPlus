# Conta+ — Contexto do Projeto (handoff)

> Documento para retomar o projeto em outra conversa. Descreve **o que** estamos
> construindo, **como** estamos trabalhando e **onde** paramos.

## 1. O que é

**Conta+** (antes "ContAI") é um copiloto financeiro com controle de estoque para
pequenos comerciantes brasileiros. Está sendo **reescrito do zero** para virar um
SaaS próprio, com dois objetivos ao mesmo tempo: ter o produto **e** aprender todo
o processo de engenharia.

- Projeto novo: `~/Documents/contaPlus` (repo separado, git do usuário pessoal).
- Referência executável: `~/Documents/contaai` (implementação TypeScript antiga; os
  `docs/` de lá são a especificação — centavos como inteiro, custo congelado na venda,
  prévia que não grava, confirmação atômica e idempotente). **Não tinha LLM de verdade**:
  a "voz" vinha do `SpeechRecognition` do navegador; transcrição é ASR, não LLM.

## 2. Stack

- **Backend:** Java 21 + Spring Boot 4.1 + Maven + PostgreSQL + Flyway + JPA/Hibernate + Testcontainers.
- **App:** Flutter para iOS e Android nativo (o antigo era Flutter web, não se reaproveita).
- **Monorepo:** `api/` (backend) e `app/` (mobile, ainda não iniciado).
- Depois, sem data: **AWS, Terraform, DevOps, mensageria**.

## 3. Como trabalhamos (modo de aprendizado — LER ANTES DE AJUDAR)

Regra central, definida pelo Eduardo:

1. **Ele escreve todo o código.** Claude explica o conceito e o porquê antes, mostra um
   exemplo comentado, e o Eduardo digita. Claude **não cria** arquivos do projeto, nem
   repo, nem commits, nem a solução no lugar dele.
   - Diagnóstico de ambiente (ler config, checar versão, ler log) é bem-vindo.
   - Escrever a implementação por ele, não.
2. **Um passo de cada vez.** Nada de despejar três blocos juntos.
3. **Exercício solo após todo exemplo feito junto.** Se fizemos `findById` juntos, o
   próximo passo é ele fazer algo equivalente (`findByName`) sozinho, fechar o Claude,
   e voltar para revisão. O solo é o que fixa o aprendizado.
4. **Poucas perguntas** quando o rumo já está decidido. Perguntar só quando leituras
   diferentes levariam a trabalhos materialmente diferentes.
5. Ao ler logs enormes do Maven, o que importa são duas coisas: o primeiro `Caused by:`
   de baixo pra cima, e a linha `Results:`/`Tests run:`. Dica: `./mvnw -q test` corta o ruído.

## 4. Estrutura atual

```
contaPlus/
├── docker-compose.yml            # Postgres 16 para desenvolvimento (raiz = infra do produto todo)
├── CONTEXTO.md                   # este arquivo
└── api/
    ├── pom.xml
    ├── src/main/java/com/contaplus/api/
    │   ├── ContaPlusApplication.java
    │   ├── health/HealthController.java        # GET /v1/health -> {"status":"ok","service":"Conta+ API"}
    │   └── store/
    │       ├── Store.java                       # @Entity da tabela stores
    │       ├── StoreRepository.java             # JpaRepository, package-private
    │       └── StoreService.java                # @Service com criar(nome) @Transactional
    ├── src/main/resources/
    │   ├── application.yaml                      # porta 8081, datasource, ddl-auto: validate
    │   └── db/migration/
    │       ├── V1__create_stores.sql
    │       └── V2__alter_stores_currency.sql
    └── src/test/java/com/contaplus/api/
        ├── ContaPlusApplicationTests.java       # contextLoads com Testcontainers
        ├── TestcontainersConfiguration.java     # @TestConfiguration que sobe Postgres 16
        ├── TestContaPlusApplication.java         # runner local com Testcontainers
        └── store/StoreRepositoryTest.java       # @SpringBootTest de integração
```

## 5. Decisões e convenções já firmadas

- **Arquitetura em camadas:** Controller -> Service -> Repository -> Entity. Chamadas
  só descem. Objetos de dados não chamam ninguém.
- **DTO** (quando entrar): é o contrato público no HTTP, nasce e morre no Controller.
  Serve para dizer *como os campos são expostos* na API sem amarrar aos nomes do banco.
  As regras (no Service) nunca veem DTO.
- **Pacote por feature** (`store/`, `health/`), não por camada. Repository fica
  package-private de propósito: encapsulamento garantido pelo compilador. Outra feature
  fala com o Service da feature vizinha, nunca com o Repository dela.
- **Flyway:** arquivos `V<n>__descricao.sql` (dois underscores). **Nunca editar uma
  migration já aplicada** — corrige-se com uma nova (foi assim que a V2 nasceu).
- **JPA:** `ddl-auto: validate` (o Flyway é dono do schema, o Hibernate só confere),
  `open-in-view: false`, construtor protegido sem-arg, entidades não são records.
- **Testes com Testcontainers**, não H2 (H2 não é Postgres). Padrão:
  `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)`.
- **Modelagem:** dinheiro como inteiro (centavos); quantidade como `numeric`;
  `timestamptz`, não `timestamp`; PK UUID gerada no Java; `store_id` em toda tabela de
  negócio desde a primeira migration (multi-inquilino não se aparafusa depois).
- **Porta 8081** (a 8080 é usada por um container de trabalho — WordPress).
- **Git:** identidade pessoal escopada por diretório via `includeIf "gitdir:"`.

## 6. Estado atual (o que já funciona)

- Docker Compose sobe o Postgres; Flyway aplica **V1 e V2**; `ddl-auto: validate` aprova.
- `GET /v1/health` responde. `/actuator/health` UP (inclui o check do banco).
- `Store` (entidade), `StoreRepository` (interface JpaRepository) e `StoreService.criar`
  existem e compilam.
- `./mvnw test` = **BUILD SUCCESS**, 2 testes: `contextLoads` e
  `deveSalvarERecuperarUmaLoja` (save + findById reais contra Postgres via Testcontainers).

### Pendências pequenas
- `StoreService` e a última mudança de `StoreRepositoryTest` ainda **não commitados**.
- Destrackear `.idea/` e `.DS_Store` (`.gitignore` na raiz + `git rm --cached`).

## 7. Próximo passo — EXERCÍCIO SOLO em aberto

**Buscar loja por nome**, no `StoreRepository`, feito pelo Eduardo sozinho.

- Só a **assinatura** do método na interface; o Spring Data implementa pelo nome do método.
  Termo para pesquisar se travar: *"Spring Data JPA derived query methods"*.
- **Funcionou quando:**
  1. Um teste salva uma loja, busca pelo nome e encontra.
  2. Um segundo teste busca um nome inexistente e **não lança exceção** — pensar em qual
     tipo de retorno permite isso (o mesmo já usado no teste atual: `Optional`).

Depois disso, Claude revisa apontando o que quebraria.

## 8. Roteiro por fases (planejado)

- **Fase 0** ✅ infra: Docker, Postgres, health, actuator.
- **Fase 1** ✅ (em fechamento) primeira feature: entidade + migration + repository +
  service + teste de integração (`store`).
- **Fase 2:** expor a feature via Controller + DTO; validação; tratamento de erro.
  Revisitar **Lombok** aqui (decisão adiada).
- **Fase 3:** modelo de negócio (produtos, estoque, vendas em centavos, custo congelado,
  confirmação atômica/idempotente).
- **Fase 3.5:** eventos de domínio na confirmação da venda (para trocar por fila depois).
- **Fase 5:** app Flutter iOS/Android (transcrição de áudio com `speech_to_text`, on-device).
- **Fase 6–7:** AWS, Terraform, DevOps, mensageria.

## 9. Comandos úteis

```bash
# subir o banco de desenvolvimento
cd ~/Documents/contaPlus && docker compose up -d

# rodar a API (porta 8081)
cd ~/Documents/contaPlus/api && ./mvnw spring-boot:run

# rodar os testes (com -q para cortar o ruído do log)
cd ~/Documents/contaPlus/api && ./mvnw -q test

# health
curl http://localhost:8081/v1/health
```
