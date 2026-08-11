# Decisões de Arquitetura — Umbra

**Ver também:** [REQUISITOS.md](./REQUISITOS.md) | [BACKLOG.md](./BACKLOG.md) | [METODOLOGIA.md](./METODOLOGIA.md)

---

## 0. Arquitetura do sistema — visão geral

```
┌─────────────────────────────────────────────────────────────────┐
│  Camada de entrada (delivery) — não decide, só aciona            │
│  scheduler/ (ticker)      api/ (REST, Chi)      dashboard/        │
└──────────────────────────────┬────────────────────────────────────┘
                                │ chama
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  usecase/ — orquestração; não sabe HTTP, ticker nem SQL           │
│  ProcessCollectedPrices · CreateAlert · DeleteAlert · ListAlerts  │
│  ListActivePromotions · GetPriceHistory · ListRecentEvents        │
└──────────┬──────────────────────────────────────┬─────────────────┘
           │ chama                                 │ chama
           ▼                                       ▼
┌─────────────────────┐                 ┌───────────────────────────┐
│  domain/             │                 │  Store / Notifier           │
│  Product, Price,      │◄────────────────┤  (interfaces)                │
│  UserAlert,           │  usam tipos    │  postgres/ , discord/        │
│  AlertSatisfied()      │  de domain     │  (implementações concretas)  │
└─────────────────────┘                 └──────────────┬────────────┘
                                                          │
                                            ┌─────────────┴─────────────┐
                                            ▼                           ▼
                                    ┌─────────────┐         ┌──────────────────┐
                                    │ PostgreSQL  │         │  Discord Webhook  │
                                    └─────────────┘         └──────────────────┘

scraper/ (steam/, nuuvem/, kabum/, pichau/) é chamado pelo scheduler/; retorna
[]domain.Product e []domain.Price, que o scheduler repassa ao usecase
ProcessCollectedPrices — o scheduler nunca fala com domain, Store nem Notifier.
```

**Fluxo de coleta:**

1. `Scheduler` dispara ticker → chama `Scraper.Collect(ctx)`
2. Scraper retorna `[]Product` e `[]Price`
3. Scheduler chama `usecase.ProcessCollectedPrices.Execute(ctx, products, prices)`
4. Use case persiste via `Store.UpsertProduct`/`Store.InsertPrice`; registra `log_evento`
5. Para cada alerta ativo do produto, chama `domain.AlertSatisfied(price, alert)` — função pura, sem I/O (ver `§3.1`)
6. Se satisfeita, chama `Store.TryClaimAlert(ctx, alertID, 24h)` — reserva atômica no banco (ver `§7.1`)
7. Se a reserva foi bem-sucedida: chama `Notifier.Send(ctx, Alert)`, registra `notificacao` e `log_evento`

**Fluxo de consulta (API REST):**

1. Request HTTP chega no handler Chi
2. Handler chama `usecase.ListActivePromotions.Execute` ou `usecase.GetPriceHistory.Execute`
3. Handler serializa o resultado para JSON e responde

**Fluxo de consulta (Dashboard):**

1. Request HTTP chega no handler de dashboard
2. Handler chama os **mesmos** use cases que a API REST usa (`ListActivePromotions`, `GetPriceHistory`, `ListAlerts`, `ListRecentEvents`)
3. Handler executa template `html/template` → HTML renderizado ao browser

---

## 1. Stack

| Camada               | Escolha                                          |
| :------------------- | :----------------------------------------------- |
| Linguagem            | Go 1.23                                          |
| HTTP server          | Chi (github.com/go-chi/chi)                      |
| HTTP client          | net/http (stdlib)                                |
| Scraping HTML        | goquery (github.com/PuerkitoBio/goquery)         |
| Persistência         | PostgreSQL 16                                    |
| Acesso ao banco      | sqlc (github.com/sqlc-dev/sqlc) sobre pgx/v5     |
| Migrations           | golang-migrate                                   |
| Frontend server-side | html/template (stdlib Go)                        |
| Testes               | testing (stdlib) + testify + testcontainers-go   |
| Linter               | golangci-lint                                    |
| CI                   | GitHub Actions                                   |
| Logging              | slog (stdlib Go 1.21+)                           |
| Containerização      | Docker + Docker Compose                          |

**Nota sobre sqlc:** escreva o SQL explícito em arquivos `.sql` (versionados em `internal/store/queries/`); sqlc gera structs e funções Go type-safe a partir deles. Código gerado vive em `internal/store/sqlc/` e nunca é importado fora de `internal/store/`.

**Nota sobre `net/http` + `goquery`:** não executam JavaScript. Funciona para fontes com HTML server-side rendered (Nuuvem, Kabum, Pichau — confirmado). Sites com proteção Cloudflare (ex. gg.deals) não são suportados — ver `§13`.

---

## 2. Estrutura de pacotes

```
umbra-scout/
  cmd/
    umbra/
      main.go             ponto de entrada; monta e liga todos os componentes
  internal/
    domain/               tipos de negócio puros (Product, Price, UserAlert, LogEvent, AlertSatisfied())
    usecase/              orquestração (ver §2.1); não sabe HTTP, ticker nem SQL
    scraper/              clientes de coleta: steam/, nuuvem/, kabum/, pichau/
    store/
      queries/            arquivos .sql com as queries — fonte de verdade, versionada
      sqlc/               código gerado por sqlc (Querier, structs) — NUNCA editado à mão, NUNCA importado fora de store/
      postgres.go         implementa Store; converte structs geradas pelo sqlc em domain.Product/domain.Price/etc.
    notifier/             integração Discord (e futuros canais)
    api/                  handlers REST JSON, DTOs de request/response — chamam usecase/
    dashboard/            handlers html/template, templates HTML — chamam usecase/
    scheduler/            agendamento periódico; só chama Scraper.Collect e usecase.Execute
    config/               leitura de variáveis de ambiente
  migrations/             arquivos .sql de migration (up/down)
  sqlc.yaml               configuração do sqlc (aponta para queries/ e schema)
  docs/                   documentação do projeto
  docker-compose.yml
  Makefile
  .env.example
```

**Separação api/ vs dashboard/:**

- `api/` → handlers que respondem JSON
- `dashboard/` → handlers que renderizam HTML com `html/template`
- Ambos chamam os **mesmos** use cases em `usecase/` — nenhum dos dois chama `Store` ou `Notifier` diretamente

**Regra de fronteira sqlc:** structs geradas pelo sqlc são diferentes das structs de `domain/`. Nada fora de `internal/store/` importa o pacote gerado. Toda conversão entre tipos sqlc e `domain` acontece dentro de `store/postgres.go`.

**Regra de dependência:**

```
domain        → nada
store         → domain
notifier      → domain
scraper       → domain
usecase       → domain, store (interface), notifier (interface)
api           → usecase
dashboard     → usecase
scheduler     → scraper, usecase
cmd/main.go   → tudo (monta as implementações concretas e injeta nos use cases)
```

---

## 2.1 Camada de Use Cases

`internal/usecase/` — um tipo por caso de uso. Cada use case:

- Recebe dependências (`Store`, `Notifier`) via injeção no construtor
- Expõe `Execute(ctx, input) (output, error)`
- Não conhece Chi, `html/template`, JSON nem SQL

```go
// usecase/create_alert.go
type CreateAlertUseCase struct {
    store Store
}

func NewCreateAlertUseCase(store Store) *CreateAlertUseCase {
    return &CreateAlertUseCase{store: store}
}

func (uc *CreateAlertUseCase) Execute(ctx context.Context, input CreateAlertInput) (domain.UserAlert, error) {
    // validação + Store.CreateAlert
    // Store.CreateAlert registra log_evento na mesma transação (ver §7)
}
```

**Use cases do MVP:**

| Use Case | Chamado por | Sprint |
|---|---|---|
| `ProcessCollectedPrices` | `scheduler/scheduler.go` | T1.5 (só persistência); avaliação+notificação em T4.3 |
| `CreateAlert` | `api/alertas.go` (`POST /alertas`) | T3.3 |
| `DeleteAlert` | `api/alertas.go` (`DELETE /alertas/{id}`) | T3.3 |
| `ListAlerts` | `api/alertas.go`, `dashboard/alertas` | T3.3, reusado em T3.5 |
| `ListActivePromotions` | `api/promocoes.go`, `dashboard/` | T2.1, reusado em T3.4 |
| `GetPriceHistory` | `api/historico.go`, `dashboard/produtos/{id}` | T2.2, reusado em T3.4 |
| `ListRecentEvents` | `dashboard/log` | T3.5 |

**Sobre testes:** handlers e scheduler são testados construindo o use case real com um `Store`/`Notifier` mockado — não é necessário mockar o use case em si.

---

## 3. Modelo de domínio

```go
// domain/product.go
type Product struct {
    ID          int
    Name        string
    Source      string    // "steam" | "kabum" | "pichau" | "nuuvem"
    SourceID    string
    Category    string    // "jogo" | "gpu" | "cpu" | "console" | ...
    URL         string
    CreatedAt   time.Time
}

// domain/price.go
type Price struct {
    ID               int
    ProductID        int
    PriceCentavos    int64     // centavos — ver §3.2
    OriginalCentavos int64
    DiscountPct      int
    CollectedAt      time.Time
}

// domain/useralert.go
type UserAlert struct {
    ID                  int
    ProductID           int
    TargetPriceCentavos *int64
    MinDiscount         *int
    UserID              *int
    LastNotifiedAt      *time.Time
    CreatedAt           time.Time
}

// domain/alert.go
type Alert struct {
    Product     Product
    Current     Price
    Previous    *Price
    TriggeredBy UserAlert
}

// domain/logevent.go
type LogEvent struct {
    ID        int
    Entity    string    // "alerta" | "preco" | "notificacao" | "produto"
    EntityID  int
    Action    string    // "criado" | "deletado"
    Payload   []byte
    UserID    *int
    CreatedAt time.Time
}
```

Tipos em `domain` são structs simples, sem anotações de banco ou JSON. Conversão acontece nas bordas (`store/postgres.go`, `api/`, `dashboard/`).

---

## 3.1 Avaliação de alerta: função pura

A condição de alerta vive em `domain` como função pura — sem I/O, sem contexto, sem dependência de infraestrutura:

```go
// domain/alert.go
func AlertSatisfied(price Price, alert UserAlert) bool {
    if alert.TargetPriceCentavos != nil && price.PriceCentavos <= *alert.TargetPriceCentavos {
        return true
    }
    if alert.MinDiscount != nil && price.DiscountPct >= *alert.MinDiscount {
        return true
    }
    return false
}
```

Comparação de inteiros — sem risco de arredondamento de `float64` (ver `§3.2`). Testável sem mocks de Store ou de contexto.

---

## 3.2 Convenção monetária: centavos como `int64`

Todo valor monetário no domínio é `int64` representando centavos — R$282,34 → `28234`. Campos terminam em `Centavos` (ex. `PriceCentavos`).

**Fronteiras de conversão:**

- **Banco → Go:** `NUMERIC(10,2)` convertido para `int64` dentro de `store/postgres.go`
- **Go → JSON:** API expõe `int64` em centavos — sem conversão na borda
- **Go → HTML:** template formata centavos para exibição (`R$ 282,34`) — responsabilidade do handler de dashboard
- **Steam API → Go:** conferir a unidade retornada antes de persistir (`T1.3`)

---

## 4. Interface Scraper

```go
// scraper/scraper.go
type Scraper interface {
    Source() string
    Collect(ctx context.Context) ([]domain.Product, []domain.Price, error)
}
```

Steam implementa via API REST. Nuuvem, Kabum e Pichau implementam via crawler HTML.

---

## 4.1 Padrão Crawler (Nuuvem, Kabum, Pichau)

```go
type Crawler struct {
    seed     string
    visited  sync.Map
    queue    chan string
    workers  int
    maxDepth int
    extract  ExtractFunc
    links    LinkFunc
}
```

**URLs semente:**

| Fonte    | URL Semente |
| :------- | :---------- |
| Nuuvem   | `https://www.nuuvem.com/br-pt/catalog/price/promo/sort/bestselling/sort-mode/desc` |
| Kabum    | `https://www.kabum.com.br/hardware` |
| Pichau   | `https://www.pichau.com.br/hardware` |

**Notas por fonte (validação manual 10/08/2026):**

- **Kabum:** HTML server-side rendered. Inclui bloco `application/ld+json` com `ItemList` — extração pode usar esse JSON além de seletores CSS.
- **Pichau:** preços em texto puro no HTML (`class="...strikeThrough">R$282.34</span>` para preço original).
- **Nuuvem:** cada card embute JSON completo em `data-default-tracker-product-tracking-data-param` (`id`, `sku`, `name`, `brand`, `price`, `url`, `image_url`, `currency`). Usar `goquery` + `json.Unmarshal` nesse atributo — mais resiliente que seletores de texto.

---

## 5. Tratamento de erros

- Erros de negócio são tipos próprios: `ErrProductNotFound`, `ErrDuplicateNotification`
- Erros de infraestrutura são wrapped: `fmt.Errorf("store.GetProduct: %w", err)`
- Falha em um scraper não cancela os demais
- Nenhuma função retorna `nil, nil` quando algo deu errado
- A camada HTTP traduz erros de negócio para status HTTP; o domínio não sabe que existe HTTP

---

## 6. Concorrência — Worker Pool

Usado na coleta de hardware (Sprints 6 e 7).

```
jobs channel  ──→  [worker 1]
                   [worker 2]  ──→  results channel
                   [worker N]
```

- N workers configurável (padrão: 5)
- Delay mínimo entre requisições para o mesmo domínio (padrão: 2s)
- `context.Context` com timeout por requisição
- `sync.WaitGroup` para aguardar todos os workers

O pool vive dentro do `Crawler` — o scheduler chama `Scraper.Collect(ctx)` e recebe o resultado pronto, sem saber que existem goroutines por trás.

---

## 7. Audit Log

Toda criação ou deleção de entidade registra um evento em `log_evento`. Inserção feita pelo `Store` após a operação principal, na mesma transação onde possível.

**Eventos registrados:**

- `POST /alertas` → `{entidade: "alerta", acao: "criado", payload: <snapshot>}`
- `DELETE /alertas/{id}` → `{entidade: "alerta", acao: "deletado"}`
- Scheduler insere preço → `{entidade: "preco", acao: "criado"}`
- Notificação enviada → `{entidade: "notificacao", acao: "criado"}`

Payload JSONB permite guardar snapshot do estado sem schema fixo. `usuario_id INT NULL` já na tabela — preenchido quando auth existir.

---

## 7.1 Idempotência de Notificações

Reserva atômica via `UPDATE ... RETURNING`:

```sql
-- store/queries/alertas.sql
-- name: TryClaimAlert :one
UPDATE alerta
SET ultima_notificacao_em = now()
WHERE id = $1
  AND (ultima_notificacao_em IS NULL OR ultima_notificacao_em < now() - $2::interval)
RETURNING id;
```

Sem linha retornada → alerta já reservado por outra avaliação → não notifica.
Com linha retornada → reserva bem-sucedida → segue para `Notifier.Send`.

A tabela `notificacao` é histórico/auditoria. A fonte de verdade da deduplicação é `alerta.ultima_notificacao_em` + essa query atômica.

---

## 8. Agendamento

```
goroutine principal
  ticker_jogos  (a cada 6h)  ──→  dispara coleta Steam + Nuuvem
  ticker_hw     (a cada 12h) ──→  dispara coleta Kabum + Pichau
```

Coleta inicial roda uma vez na startup antes do primeiro tick. `context.Context` com cancelamento permite parar os tickers via SIGTERM.

---

## 9. Configuração

```
UMBRA_DATABASE_URL          postgresql://user:pass@db:5432/umbra
UMBRA_DISCORD_WEBHOOK       https://discord.com/api/webhooks/...
UMBRA_MIN_STORAGE_DISCOUNT  5
UMBRA_WORKER_COUNT          5
UMBRA_COLLECT_GAMES_H       6
UMBRA_COLLECT_HW_H          12
```

- Variáveis obrigatórias ausentes → aplicação falha na inicialização com mensagem clara
- `.env.example` versionado com valores fictícios; `.env` real no `.gitignore`
- Nenhuma credencial no código-fonte

`UMBRA_MIN_STORAGE_DISCOUNT` filtra produtos com desconto abaixo do limiar para não poluir o banco. Não controla notificações — notificações são controladas pelos alertas individuais.

---

## 10. Testes

| Tipo                    | Alvo                                                        | Ferramenta        | Toca banco?         |
| :---------------------- | :---------------------------------------------------------- | :---------------- | :------------------ |
| Unitário                | domain, lógica de filtragem, formatação Discord             | testing + testify | Não                 |
| Integração de store     | queries SQL, migrations, constraints                        | testcontainers-go | Sim                 |
| Integração de API       | endpoints HTTP REST                                         | httptest (stdlib) | Não (mock do store) |
| Integração de dashboard | handlers html/template                                      | httptest (stdlib) | Não (mock do store) |

### 10.1 Cenários obrigatórios por sprint

**Sprint 1 (Steam)**

- `Collect` retorna lista não vazia para resposta válida
- `Collect` retorna erro descritivo para resposta inválida (não JSON, status 5xx)
- Produto com desconto abaixo do mínimo não entra nos resultados
- Upsert de produto existente não gera erro nem duplicata

**Sprint 3 (Dashboard + Alertas)**

- `POST /alertas` sem `preco_alvo` nem `desconto_min` retorna 400
- `DELETE /alertas/{id}` remove o alerta e registra evento em `log_evento`
- Dashboard `/dashboard` renderiza HTML com lista de promoções
- Criação e deleção de alerta aparecem em `/dashboard/log`

**Sprint 4 (Discord)**

- Mensagem formatada corretamente com preço, desconto e condição que disparou
- `domain.AlertSatisfied` avalia todos os casos de borda (`preco_alvo` só, `desconto_min` só, ambos, nenhum)
- `TryClaimAlert` impede duplicação mesmo sob avaliações concorrentes (`go test -race`)
- Falha no webhook não derruba o serviço

**Sprint 6 (Hardware / concorrência)**

- Worker pool com N=3 sem race condition (`go test -race`)
- Timeout por requisição respeitado
- Falha em um produto não cancela os demais

### 10.2 Metas de cobertura

| Escopo                     |  Meta   |
| :------------------------- | :-----: |
| domain                     |   85%   |
| scraper (com mock HTTP)    |   70%   |
| store (com testcontainers) |   75%   |
| **Projeto**                | **70%** |

---

## 11. Estratégia de branches e CI

- `main` — protegida, sempre passando no CI. Sem push direto.
- `feature/<task-id>-<descricao>` — uma branch por task
- CI em todo PR: `go build ./...`, `go test ./...`, `go vet ./...`, `golangci-lint run`
- Merge bloqueado se CI falhar

---

## 12. Containerização

**Dockerfile multi-stage:**

- Estágio build: imagem com Go SDK completo, compila o binário
- Estágio runtime: `gcr.io/distroless/static` — imagem mínima, só o binário
- Aplicação roda com usuário não-root

**Docker Compose:**

| Serviço | Papel |
| :------ | :---- |
| `db`    | PostgreSQL 16 com volume nomeado |
| `app`   | Umbra, dependente do healthcheck do banco |

---

## 13. Registro de decisões

| Data       | Decisão | Custo aceito |
| :--------- | :------ | :----------- |
| 10/08/2026 | **gg.deals removido do MVP.** Retorna Cloudflare challenge (`_cf_chl_opt`) — incompatível com `net/http`+`goquery`. URL semente da Nuuvem corrigida para `/br-pt/catalog/price/promo/sort/bestselling/sort-mode/desc` (anterior retornava 404). | Perda de cobertura de um agregador multi-loja. Reintrodução futura exigiria headless browser e mudança de stack. |
| 10/08/2026 | **Persistência via sqlc** sobre pgx/v5. GORM descartado. Fronteira explícita: código gerado em `internal/store/sqlc/`, nunca importado fora de `internal/store/`. | `sqlc generate` precisa rodar antes de compilar — passo extra no fluxo de build. |
| 10/08/2026 | **`domain.AlertSatisfied` extraído para função pura** em `domain/`. Scheduler chama a função, não reimplementa a condição. | Mais um conceito de camada a gerenciar (domain com comportamento, não só dados). |
| 10/08/2026 | **Idempotência via `UPDATE ... RETURNING` atômico** com coluna `alerta.ultima_notificacao_em`. Substitui padrão check-then-act vulnerável a condição de corrida. | Coluna extra no schema de `alerta`; duas fontes de verdade relacionadas (`alerta.ultima_notificacao_em` + tabela `notificacao`). |
| 10/08/2026 | **Valores monetários como `int64` centavos.** `shopspring/decimal` avaliado e descartado. | Conversão explícita (×100 / ÷100) em toda leitura e escrita de preço nas bordas. |
| 10/08/2026 | **Camada `internal/usecase/` adotada.** `api/`, `dashboard/` e `scheduler/` passam a depender só de `usecase/`, nunca de `Store`/`Notifier` diretamente. | ~7 arquivos novos em `usecase/`, incluindo casos de uso que hoje são pouco mais que repasse direto ao `Store`. Custo aceito como objetivo de aprendizado explícito. |

---

_Última atualização: 10/08/2026_
