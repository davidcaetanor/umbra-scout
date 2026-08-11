# 📋 Backlog — Umbra

**Ver também:** [REQUISITOS.md](./REQUISITOS.md) | [ARCHITECTURE.md](./ARCHITECTURE.md) | [METODOLOGIA.md](./METODOLOGIA.md)

**Stack:** Go 1.23 · PostgreSQL 16 · sqlc · pgx · Chi · goquery · Docker
**Status:** 🔲 Pronto para iniciar

**Progresso:** Sprint 0 pendente · **0 de 45 tasks (0%)**

*Última atualização: 11/08/2026 — v2.0: T0.7 expandido com jobs de CI completos: go test -race, sqlc generate + git diff --exit-code e golangci-lint; conceitos adicionados para race detector e verificação de arquivos gerados*

---

## Legenda

**Nível**
- 🟢 Go básico — structs, funções, error handling, tipos
- 🟡 Go intermediário — interfaces, HTTP client/server, SQL com pgx, testes
- 🔴 Go avançado — goroutines, channels, worker pool, crawler com sync.Map

**Dependências:** `↳ Depende de: TN.N` — só começa depois da task listada estar na `main`.

**Independência:** cada task lista **Arquivos tocados** — tasks sem dependência entre si não tocam os mesmos arquivos. Qualquer dev pega qualquer task livre.

---

## Visão geral das sprints

| Sprint | Tema | Conceito Go introduzido |
|:-------|:-----|:------------------------|
| 0 | Fundação | Módulos, config, HTTP básico, Docker, CI |
| 1 | Steam API | HTTP client, JSON, interfaces, sqlc, usecase pattern, goroutine simples |
| 2 | API REST | HTTP server, handlers, middleware, testes de API |
| 3 | Dashboard + Alertas | html/template, CRUD, audit log, JSONB, reuso de usecase |
| 4 | Discord | HTTP POST, webhook, idempotência atômica, error handling não-crítico |
| 5 | Crawler — Jogos | Crawler pattern, sync.Map, descoberta dinâmica de URLs |
| 6 | Crawler — Kabum | Worker pool, rate limiting, context com timeout |
| 7 | Crawler — Pichau | Consolidação: mesmo padrão, HTML diferente |
| 8 | Polimento | slog, linter, cobertura, README |

---

## Sprint 0 — Fundação do Projeto

**Objetivo:** repositório configurado, banco subindo, servidor HTTP respondendo, CI verde. Zero lógica de negócio.

**Sequência:** T0.1 (gate) → T0.2, T0.3, T0.5, T0.6, T0.7 em paralelo → T0.4 após T0.2 → T0.8 após T0.2+T0.5

---

- [ ] **T0.1 — Módulo Go e estrutura de diretórios** 🟢

  `go mod init github.com/SEU_USER/Umbra`. Criar estrutura de `ARCHITECTURE.md §2` com `.gitkeep`. Commitar.

  **⚠️ Gate da sprint.**
  **Arquivos tocados:** `go.mod`, estrutura de pastas, `.gitignore`

  **Conceitos a estudar:**
  - `go mod init`, `go.mod`, `go.sum`, `go mod tidy` → [Using Go Modules](https://go.dev/blog/using-go-modules)
  - `package main` vs outros packages; `internal/` = privado ao módulo
  - `cmd/` para binários, `internal/` para código não-público

---

- [ ] **T0.2 — Docker Compose com PostgreSQL** 🟢

  `docker-compose.yml` com serviço `db` (PostgreSQL 16, volume nomeado, healthcheck com `pg_isready`).

  *↳ Depende de: T0.1*
  **Arquivos tocados:** `docker-compose.yml`

  **Conceitos a estudar:**
  - Docker: imagem vs container, volume nomeado
  - `healthcheck`: por que sem isso o `app` pode subir antes do banco estar pronto
  - Connection string: `postgresql://user:pass@host:port/dbname`

---

- [ ] **T0.3 — Config por variáveis de ambiente** 🟢

  `internal/config/config.go`: struct `Config`, `Load() (Config, error)`. Falha clara se variável obrigatória ausente. Criar `.env.example`.

  *↳ Depende de: T0.1*
  **Arquivos tocados:** `internal/config/config.go`, `.env.example`

  **Conceitos a estudar:**
  - `os.Getenv` vs `os.LookupEnv` — diferença real
  - Campos exportados em struct (maiúsculo) vs não exportados
  - `fmt.Errorf("config: variável %s ausente", name)`

---

- [ ] **T0.4 — Primeira migration** 🟢

  golang-migrate. Migration com as tabelas `produto` e `preco` de `REQUISITOS.md §7`. `make migrate-up` e `make migrate-down` funcionando. As tabelas `alerta`, `notificacao` e `log_evento` chegam nas sprints seguintes.

  *↳ Depende de: T0.2*
  **Arquivos tocados:** `migrations/000001_init.up.sql`, `migrations/000001_init.down.sql`

  **Conceitos a estudar:**
  - Por que migrations: schema versionado junto com o código
  - `000001_nome.up.sql` / `000001_nome.down.sql`
  - DDL básico: `CREATE TABLE`, `SERIAL`, `REFERENCES`, `UNIQUE(a,b)`, `TIMESTAMPTZ`, `NUMERIC`

---

- [ ] **T0.5 — Servidor HTTP mínimo com healthcheck** 🟢

  `cmd/Umbra/main.go` sobe servidor Chi. `GET /health` retorna `{"status":"ok"}`. Porta via config.

  *↳ Depende de: T0.1, T0.3*
  **Arquivos tocados:** `cmd/Umbra/main.go`

  **Conceitos a estudar:**
  - `net/http`: `http.Handler`, `http.HandlerFunc`, `http.ListenAndServe`
  - Chi: `chi.NewRouter()`, `r.Get("/health", handler)`
  - `json.NewEncoder(w).Encode(v)`

---

- [ ] **T0.6 — Makefile** 🟢

  Targets: `run`, `build`, `test`, `lint`, `migrate-up`, `migrate-down`, `docker-up`, `docker-down`, `sqlc-generate`.

  *↳ Depende de: T0.1*
  **Arquivos tocados:** `Makefile`

  **Conceitos a estudar:**
  - Makefile: target, recipe, variável, `.PHONY`
  - Por que padronizar o workflow entre devs
  - `sqlc-generate` roda `sqlc generate` — precisa rodar de novo toda vez que uma query `.sql` mudar

---

- [ ] **T0.7 — GitHub Actions CI** 🟢

  `.github/workflows/ci.yml`: roda em todo PR com os seguintes jobs:
  - `go build ./...` — garante que o código compila
  - `go vet ./...` — detecta erros que o compilador ignora (ex: printf com args errados)
  - `go test -race ./...` — roda toda a suite com race detector habilitado; identifica acessos concorrentes não sincronizados. Essencial desde o início — race conditions introduzidas cedo são difíceis de rastrear depois
  - `sqlc generate && git diff --exit-code` — verifica que o código gerado em `internal/store/sqlc/` está sincronizado com os arquivos `.sql`. Sem esse job, um dev pode editar uma query e esquecer de rodar `sqlc generate`; o CI passa mas o código está desatualizado
  - `golangci-lint run` — linter agregado; detecta problemas de estilo e segurança além do `go vet`

  *↳ Depende de: T0.1*
  **Arquivos tocados:** `.github/workflows/ci.yml`

  **Conceitos a estudar:**
  - GitHub Actions: `on: pull_request`, `jobs`, `steps`, `actions/setup-go`
  - `go vet`: o que detecta além do compilador
  - Race detector (`-race`): por que instrumenta o binário e tem custo de execução (~2–20x mais lento) — justifica rodar em CI mas não sempre em dev local
  - `git diff --exit-code`: retorna código de saída não-zero se houver qualquer diferença no working tree — forma idiomática de verificar que arquivos gerados estão atualizados no CI

---

- [ ] **T0.8 — Dockerfile multi-stage + compose completo** 🟢

  Estágio build (Go SDK) + estágio runtime (distroless). Serviço `app` no compose dependente do healthcheck.

  *↳ Depende de: T0.2, T0.5*
  **Arquivos tocados:** `Dockerfile`, `docker-compose.yml` (serviço `app`)

  **Conceitos a estudar:**
  - `FROM ... AS builder`, `COPY --from=builder` — imagem final sem SDK
  - `gcr.io/distroless/static`: apenas o binário
  - `depends_on` com `condition: service_healthy`

---

## Sprint 1 — Domínio + Steam API

**Objetivo:** coleta automática de promoções Steam a cada 6h persiste no banco.

**Sequência:** T1.1 → T1.2 (gates) → T1.3, T1.4, T1.5 em paralelo → T1.6 → T1.7

---

- [ ] **T1.1 — Tipos de domínio** 🟢

  `internal/domain/product.go`, `price.go`, `useralert.go`, `alert.go`, `logevent.go`. Structs puras de `ARCHITECTURE.md §3`. Zero anotações.

  *↳ Depende de: T0.1*
  **⚠️ Gate da sprint.**
  **Arquivos tocados:** `internal/domain/`

  **Conceitos a estudar:**
  - Structs: campos, tipos primitivos, zero values, ponteiros opcionais (`*int64`, `*int`, `*time.Time`)
  - `time.Time`, `time.Now().UTC()`
  - Valores monetários são `int64` em centavos (`PriceCentavos`, `TargetPriceCentavos`), nunca `float64` — ver `ARCHITECTURE.md §3.2` para o porquê
  - Por que sem anotações: domínio não sabe que existe PostgreSQL ou JSON

---

- [ ] **T1.2 — Interfaces `Scraper` e `Store`** 🟡

  `internal/scraper/scraper.go` com `Scraper`. `internal/store/store.go` com `Store` (inclui `LogEvento`, `TryClaimAlert`). Só interfaces.

  *↳ Depende de: T1.1*
  **⚠️ Gate — todos esperam na `main`.**
  **Arquivos tocados:** `internal/scraper/scraper.go`, `internal/store/store.go`

  **Conceitos a estudar:**
  - Interfaces em Go: satisfação implícita, diferença de Java
  - `context.Context`: por que toda I/O deveria receber um context
  - Definir interface antes da implementação

---

- [ ] **T1.3 — Scraper Steam** 🟡

  `internal/scraper/steam/steam.go` implementando `Scraper`. Steam Store API `/api/featuredcategories`. Filtro de desconto mínimo de armazenamento.

  *↳ Depende de: T1.2, T0.3*
  **Arquivos tocados:** `internal/scraper/steam/steam.go`

  **Conceitos a estudar:**
  - `http.NewRequestWithContext`: request com context
  - `defer resp.Body.Close()` — leak de conexão se não fechar
  - Struct tags JSON: `json:"campo"`, `json:",omitempty"`
  - `json.NewDecoder(resp.Body).Decode(&v)`
  - Conferir a unidade de preço retornada pela Steam API antes de converter para `domain.Price.PriceCentavos` (`ARCHITECTURE.md §3.2`) — não assumir que já vem em centavos sem checar

---

- [ ] **T1.4 — Store PostgreSQL com sqlc** 🟡

  Escrever as queries em `internal/store/queries/*.sql` (upsert de produto, insert de preço, get produto, list preços). Configurar `sqlc.yaml` na raiz. Rodar `sqlc generate` para gerar o código em `internal/store/sqlc/`. Implementar `internal/store/postgres.go` satisfazendo a interface `Store`, convertendo as structs geradas pelo sqlc para `domain.Product`/`domain.Price`/etc. Métodos: `UpsertProduct`, `InsertPrice`, `GetProduct`, `ListPrices`, `LogEvento`.

  *↳ Depende de: T1.2, T0.4*
  **Arquivos tocados:** `internal/store/queries/*.sql`, `sqlc.yaml`, `internal/store/sqlc/` (gerado — não editar à mão), `internal/store/postgres.go`

  **Conceitos a estudar:**
  - Fluxo sqlc: escrever `.sql` com anotação `-- name: NomeDaQuery :one/:many/:exec` → `sqlc generate` → usar o código gerado
  - Upsert: `INSERT ... ON CONFLICT (fonte, identificador) DO UPDATE SET ...` (escrito como SQL puro no arquivo de query)
  - Regra de fronteira (`ARCHITECTURE.md §2`): código gerado pelo sqlc nunca é importado fora de `internal/store/` — a conversão pra `domain` acontece só em `postgres.go`
  - Conversão `NUMERIC(10,2)` (Postgres) ↔ `int64` centavos (`domain.Price`): acontece dentro de `postgres.go`, nunca no `domain` (`ARCHITECTURE.md §3.2`)
  - `json.Marshal` para serializar payload do `log_evento` como JSONB

---

- [ ] **T1.5 — Scheduler + primeiro Use Case: `ProcessCollectedPrices`** 🔴

  `internal/scheduler/scheduler.go`. `time.Ticker` em goroutine. Coleta inicial na startup. Encerramento limpo via context. Criar `internal/usecase/process_collected_prices.go` com `ProcessCollectedPricesUseCase` — recebe `[]domain.Product` e `[]domain.Price` do scraper e persiste via `Store` (`UpsertProduct`, `InsertPrice`). Nesta sprint ainda **não** avalia alertas (isso chega na `T4.3`, quando `Notifier` e `TryClaimAlert` existirem) — mas o scheduler já chama só `Scraper.Collect` e `usecase.Execute`, nunca `Store` diretamente, desde já (ver `ARCHITECTURE.md §2.1`).

  *↳ Depende de: T1.2*
  **Arquivos tocados:** `internal/scheduler/scheduler.go`, `internal/usecase/process_collected_prices.go`

  **Conceitos a estudar:**
  - Goroutines: `go func()`, por que são baratas
  - `time.NewTicker`: `ticker.C`, `ticker.Stop()`
  - `context.WithCancel`, `defer cancel()`
  - `select`: `case <-ticker.C:` vs `case <-ctx.Done():`
  - Por que o scheduler não chama `Store` diretamente: a partir desta task, toda orquestração de negócio vive em `usecase/` (`ARCHITECTURE.md §2.1`) — o scheduler só aciona

---

- [ ] **T1.6 — Ligação no `main.go`** 🟡

  Instanciar config, store, scraper Steam, o usecase `ProcessCollectedPrices` (injetando o `Store`) e o scheduler (injetando o usecase, não o `Store`, no scheduler). SIGTERM para shutdown limpo.

  *↳ Depende de: T1.3, T1.4, T1.5*
  **Arquivos tocados:** `cmd/Umbra/main.go`

  **Conceitos a estudar:**
  - Injeção de dependência manual: instanciar na `main`, passar como parâmetro
  - `signal.NotifyContext`: capturar SIGTERM/SIGINT
  - Ordem: banco → store → scheduler

---

- [ ] **T1.7 — Testes da Sprint 1** 🟡

  Scraper Steam com `httptest.NewServer`. Store com testcontainers. Cenários de `ARCHITECTURE.md §10.1`.

  *↳ Depende de: T1.3, T1.4*
  **Arquivos tocados:** `internal/scraper/steam/steam_test.go`, `internal/store/postgres_test.go`

  **Conceitos a estudar:**
  - `httptest.NewServer`: mock sem bater na Steam real
  - testcontainers-go: PostgreSQL real, `TestMain` para container único
  - `go test -race`

---

## Sprint 2 — API REST

**Objetivo:** dados expostos via endpoints JSON.

**Sequência:** T2.1, T2.2, T2.3 independentes → T2.4 integra tudo.

---

- [ ] **T2.1 — `GET /promocoes`** 🟡

  Criar `internal/usecase/list_active_promotions.go` com `ListActivePromotionsUseCase` (recebe filtros `tipo`/`desconto_min`, chama `Store.ListPromotions`). Handler `internal/api/promocoes.go` chama o usecase — não o `Store` diretamente. Filtros `tipo` e `desconto_min` via query params. JSON com produtos em promoção.

  *↳ Depende de: T1.4*
  **Arquivos tocados:** `internal/usecase/list_active_promotions.go`, `internal/api/promocoes.go`

  **Conceitos a estudar:**
  - `r.URL.Query().Get("param")`, `strconv.Atoi`
  - Struct de response com tags `json:"campo"`
  - `http.StatusOK` (200) vs `http.StatusBadRequest` (400)
  - Este usecase será reusado pelo dashboard na `T3.4` — mesma consulta, apresentação diferente (`ARCHITECTURE.md §2.1`)

---

- [ ] **T2.2 — `GET /produtos/{id}/historico`** 🟡

  Criar `internal/usecase/get_price_history.go` com `GetPriceHistoryUseCase` (chama `Store.ListPrices`, calcula preço mínimo histórico e variação percentual). Handler `internal/api/historico.go` chama o usecase. Série temporal: preço mínimo histórico, variação percentual.

  *↳ Depende de: T1.4*
  **Arquivos tocados:** `internal/usecase/get_price_history.go`, `internal/api/historico.go`

  **Conceitos a estudar:**
  - `chi.URLParam(r, "id")`
  - SQL: `ORDER BY coletado_em ASC`, `MIN(preco_brl)`
  - `http.StatusNotFound` (404): produto inexistente vs sem histórico
  - Cálculo de "preço mínimo histórico" e "variação percentual" é regra de apresentação de dado, não de decisão — cabe no usecase, que devolve os números já prontos pro handler formatar

---

- [ ] **T2.3 — Middleware de logging e erros** 🟡

  Middleware Chi de logging. Handler centralizado: traduz erros de domínio em status HTTP.

  *↳ Depende de: T0.5*
  **Arquivos tocados:** `internal/api/middleware.go`, `internal/api/errors.go`

  **Conceitos a estudar:**
  - Middleware: `func(next http.Handler) http.Handler`
  - `slog.Info("request", "method", r.Method, "status", status)`
  - `errors.Is` / `errors.As`

---

- [ ] **T2.4 — Registrar rotas e testes da Sprint 2** 🟡

  Registrar handlers no router. Testes com `httptest.NewRecorder`, construindo os usecases reais com `Store` mockado (não é preciso mockar o usecase em si — ver `ARCHITECTURE.md §2.1`).

  *↳ Depende de: T2.1, T2.2, T2.3*
  **Arquivos tocados:** `cmd/Umbra/main.go`, `internal/api/promocoes_test.go`, `internal/api/historico_test.go`

  **Conceitos a estudar:**
  - `httptest.NewRecorder`: testar handler sem servidor real
  - `mockStore`: struct que implementa `Store` nos testes — injetada no usecase real, não no handler
  - `assert.JSONEq`

---

## Sprint 3 — Dashboard + Alertas Personalizados

**Objetivo:** usuário consegue ver promoções no browser, criar alertas por produto e acompanhar a atividade do sistema via audit log.

**Sequência:** T3.1 (gate) → T3.2 → T3.3 e T3.4 em paralelo → T3.5 após T3.3 → T3.6 integra tudo.

---

- [ ] **T3.1 — Migration: `alerta` e `log_evento`** 🟢

  Criar `migrations/000002_alertas_log.up.sql` com as tabelas `alerta` e `log_evento` de `REQUISITOS.md §7`. Note que `alerta` já inclui a coluna `ultima_notificacao_em` (usada só a partir da Sprint 4, mas faz parte do schema desde já). Down desfaz na ordem inversa.

  *↳ Depende de: T0.4*
  **⚠️ Gate da sprint.**
  **Arquivos tocados:** `migrations/000002_alertas_log.up.sql`, `migrations/000002_alertas_log.down.sql`

  **Conceitos a estudar:**
  - `CHECK (preco_alvo IS NOT NULL OR desconto_min IS NOT NULL)`: constraint que garante pelo menos um campo preenchido
  - `JSONB` vs `TEXT` para payload: JSONB permite queries dentro do JSON no futuro
  - `INT NULL` para `usuario_id`: por que nullable agora facilita adicionar auth depois sem reescrita
  - `ultima_notificacao_em TIMESTAMPTZ NULL`: por que essa coluna vive em `alerta` e não só em `notificacao` — ela é a fonte de verdade da idempotência (`ARCHITECTURE.md §7.1`), usada apenas na Sprint 4

---

- [ ] **T3.2 — Store: CRUD de alertas e LogEvento** 🟡

  Escrever as queries de alertas e log em `internal/store/queries/*.sql`, rodar `sqlc generate`. Implementar em `internal/store/postgres.go`: `CreateAlert`, `ListAlerts`, `DeleteAlert`, `ListActiveAlerts(ctx, productID)`, `LogEvento`, `ListLogEvents`.

  *↳ Depende de: T3.1, T1.4*
  **Arquivos tocados:** `internal/store/queries/alertas.sql`, `internal/store/queries/log.sql`, `internal/store/postgres.go`

  **Conceitos a estudar:**
  - sqlc: query com `-- name: CreateAlert :one` e `RETURNING id` para obter o ID gerado
  - `encoding/json`: `json.Marshal(alerta)` para o payload do `log_evento`
  - Transação: `conn.Begin()` → operação principal → `LogEvento` → `Commit()`; por que na mesma transação

---

- [ ] **T3.3 — Endpoints REST: alertas** 🟡

  Criar `internal/usecase/create_alert.go` (`CreateAlertUseCase` — valida que ao menos um de `preco_alvo`/`desconto_min` foi preenchido, chama `Store.CreateAlert`), `internal/usecase/delete_alert.go` (`DeleteAlertUseCase` — chama `Store.DeleteAlert`) e `internal/usecase/list_alerts.go` (`ListAlertsUseCase` — chama `Store.ListAlerts`). Handlers `POST /alertas`, `GET /alertas`, `DELETE /alertas/{id}` chamam os usecases correspondentes — não o `Store` diretamente. Retorna `201 Created` no POST.

  *↳ Depende de: T3.2*
  **Arquivos tocados:** `internal/usecase/create_alert.go`, `internal/usecase/delete_alert.go`, `internal/usecase/list_alerts.go`, `internal/api/alertas.go`

  **Conceitos a estudar:**
  - `json.NewDecoder(r.Body).Decode(&req)`: ler body do request
  - Validação: verificar se pelo menos um campo foi enviado — essa regra mora no usecase, não no handler nem no `Store`
  - `http.StatusCreated` (201) vs `http.StatusOK` (200): a diferença semântica importa em REST
  - `ListAlertsUseCase` será reusado pelo dashboard na `T3.5` (`GET /dashboard/alertas`)

---

- [ ] **T3.4 — Dashboard: página inicial e produto** 🟡

  `internal/dashboard/dashboard.go` com handlers. Templates em `internal/dashboard/templates/`. **Reusa os usecases criados na Sprint 2** (`ListActivePromotionsUseCase`, `GetPriceHistoryUseCase`) — não reimplementa a consulta, só troca a apresentação (JSON → HTML).
  - `GET /dashboard` — tabela de promoções ativas com link para cada produto
  - `GET /dashboard/produtos/{id}` — histórico de preços com gráfico (Chart.js via CDN)

  *↳ Depende de: T1.4, T2.1, T2.2*
  **Arquivos tocados:** `internal/dashboard/dashboard.go`, `internal/dashboard/templates/home.html`, `internal/dashboard/templates/produto.html`

  **Conceitos a estudar:**
  - `html/template`: `template.ParseFiles`, `template.Execute(w, data)` — diferença de `text/template`
  - Ações de template: `{{range .Produtos}}`, `{{.Nome}}`, `{{if .DescontoPct}}`
  - XSS: por que `html/template` escapa automaticamente e `text/template` não
  - Chart.js via CDN: passar dados do Go para o template como JSON inline no `<script>`
  - Formatação de centavos para exibição (`28234` → `"R$ 282,34"`): a conversão é do handler/dashboard, nunca do `domain` (`ARCHITECTURE.md §3.2`)
  - Este é o primeiro lugar onde o reuso do usecase se prova na prática — mesma consulta, dois formatos de saída, zero duplicação de lógica

---

- [ ] **T3.5 — Dashboard: alertas e log** 🟡

  - `GET /dashboard/alertas` — lista alertas ativos reusando `ListAlertsUseCase` (`T3.3`); formulário para criar novo alerta faz `POST /alertas` — vai pro endpoint REST (padrão PRG), não duplica a criação chamando o usecase de outro lugar
  - `GET /dashboard/log` — feed de eventos recentes. Criar `internal/usecase/list_recent_events.go` (`ListRecentEventsUseCase` — chama `Store.ListLogEvents`)

  *↳ Depende de: T3.3, T3.4*
  **Arquivos tocados:** `internal/usecase/list_recent_events.go`, `internal/dashboard/templates/alertas.html`, `internal/dashboard/templates/log.html`

  **Conceitos a estudar:**
  - HTML form com `method="POST" action="/alertas"`: como um formulário chama a API REST
  - `r.ParseForm()`, `r.FormValue("preco_alvo")`: ler dados de formulário em Go
  - Redirect após POST: `http.Redirect(w, r, "/dashboard/alertas", http.StatusSeeOther)` — padrão PRG (Post/Redirect/Get)
  - Por que o formulário de criação não chama `CreateAlertUseCase` direto do handler de dashboard: reusar o endpoint REST evita ter dois caminhos de código fazendo a mesma coisa

---

- [ ] **T3.6 — Registrar rotas do dashboard e testes** 🟡

  Montar rotas `/dashboard/*` no router principal. Testes dos handlers de dashboard com `httptest.NewRecorder` e store mockado. Testes dos endpoints de alerta.

  *↳ Depende de: T3.3, T3.5*
  **Arquivos tocados:** `cmd/Umbra/main.go`, `internal/dashboard/dashboard_test.go`, `internal/api/alertas_test.go`

  **Conceitos a estudar:**
  - Testar handler de template: verificar status 200 e presença de substring no body HTML
  - Testar POST /alertas sem campos obrigatórios: verificar 400
  - `mockStore`: mesmo padrão da Sprint 2, agora com métodos de alerta e log

---

## Sprint 4 — Discord + Disparo de Alertas

**Objetivo:** quando condição de alerta é satisfeita na coleta, notificação chega no canal Discord — de forma idempotente, mesmo sob concorrência.

**Sequência:** T4.1 e T4.2 independentes → T4.3 integra → T4.4 testa.

---

- [ ] **T4.1 — Cliente Discord** 🟡

  `internal/notifier/discord/discord.go` implementando interface `Notifier`. HTTP POST para webhook com embed rico: nome, fonte, preço atual, preço original, desconto, condição que disparou e link.

  *↳ Depende de: T0.3, T1.1*
  **Arquivos tocados:** `internal/notifier/discord/discord.go`, `internal/notifier/notifier.go` (interface)

  **Conceitos a estudar:**
  - Discord Webhook: payload com `embeds`, campos `title`, `description`, `color`, `url`
  - `json.Marshal` → `bytes.NewBuffer` → body do POST
  - `resp.StatusCode == 204`: sucesso no Discord; qualquer outro é erro
  - Formatação de centavos para a mensagem (`28234` → `"R$ 282,34"`) acontece aqui, na borda de apresentação — não no `domain` (`ARCHITECTURE.md §3.2`)

---

- [ ] **T4.2 — Migration de notificações + reserva atômica de alerta** 🔴

  `migrations/000003_notificacoes.up.sql` com a tabela `notificacao` (histórico/auditoria — ver `ARCHITECTURE.md §7.1`). Adicionar ao `Store`: `InsertNotification` e `TryClaimAlert(ctx, alertID int, window time.Duration) (bool, error)`, implementado via `UPDATE alerta SET ultima_notificacao_em = now() WHERE id = $1 AND (ultima_notificacao_em IS NULL OR ultima_notificacao_em < now() - $2::interval) RETURNING id` (escrito como query sqlc). Retorna `true` só se uma linha foi afetada.

  *↳ Depende de: T3.1, T1.4*
  **Arquivos tocados:** `internal/store/queries/notificacoes.sql`, `internal/store/queries/alertas.sql`, `internal/store/postgres.go`, `migrations/000003_notificacoes.up.sql`, `migrations/000003_notificacoes.down.sql`

  **Conceitos a estudar:**
  - Padrão *check-then-act* (`SELECT` depois `INSERT`) vs `UPDATE ... RETURNING` atômico: por que o primeiro é vulnerável a condição de corrida entre duas avaliações do mesmo alerta, e o segundo não
  - `RETURNING`: como usar o número de linhas afetadas (0 ou 1) como sinal de "a reserva foi minha ou de outro processo"
  - `notificacao` guarda histórico para exibição (`/dashboard/log`); `alerta.ultima_notificacao_em` é a fonte de verdade da deduplicação — são responsabilidades diferentes

---

- [ ] **T4.3 — Expandir `ProcessCollectedPrices`: avaliação e notificação** 🔴

  Adicionar `domain.AlertSatisfied(price, alert) bool` em `internal/domain/alert.go` — função pura, sem I/O (ver `ARCHITECTURE.md §3.1`). Expandir `internal/usecase/process_collected_prices.go` (criado em `T1.5`): depois de persistir o preço, buscar alertas ativos via `Store.ListActiveAlerts(ctx, productID)`; para cada alerta, chamar `domain.AlertSatisfied` (o usecase **não reimplementa** a condição); se satisfeita, chamar `Store.TryClaimAlert(ctx, alert.ID, 24*time.Hour)`; só se a reserva for bem-sucedida, chamar `Notifier.Send` e registrar `notificacao` + `log_evento`. Falha no Discord não derruba o serviço.

  **O `scheduler.go` não precisa ser tocado nesta task** — ele já só chama `Scraper.Collect` e `usecase.Execute` desde a `T1.5`; só o usecase cresce.

  *↳ Depende de: T4.1, T4.2, T1.5*
  **Arquivos tocados:** `internal/domain/alert.go`, `internal/usecase/process_collected_prices.go`, `cmd/Umbra/main.go` (injetar `Notifier` no usecase)

  **Conceitos a estudar:**
  - Por que `AlertSatisfied` vive em `domain` e não em `usecase`: testável sem mocks, sem contexto, sem infraestrutura — só recebe `domain.Price` e `domain.UserAlert`, devolve `bool`
  - Error handling não-crítico: `slog.Error(...)` e continuar — não propagar o erro para cima
  - `context.WithTimeout` para chamada ao Discord: webhook lento não trava o usecase
  - Usecase recebe `Notifier` como interface: não sabe que existe Discord

---

- [ ] **T4.4 — Testes da Sprint 4** 🔴

  Cliente Discord com mock webhook (`httptest.NewServer`). `domain.AlertSatisfied` com testes unitários cobrindo todos os casos de borda. `TryClaimAlert` com testcontainers, incluindo teste de concorrência (`go test -race`, várias goroutines chamando `TryClaimAlert` pro mesmo alerta ao mesmo tempo — só uma pode ter sucesso). Cenários de `ARCHITECTURE.md §10.1`.

  *↳ Depende de: T4.1, T4.2, T4.3*
  **Arquivos tocados:** `internal/notifier/discord/discord_test.go`, `internal/domain/alert_test.go`, `internal/usecase/process_collected_prices_test.go`, `internal/store/postgres_test.go`

  **Conceitos a estudar:**
  - `httptest.NewServer` retornando 500: verificar que o serviço não quebra
  - Teste de concorrência real: disparar N goroutines chamando `TryClaimAlert` simultaneamente e verificar que exatamente uma teve sucesso
  - Testar `AlertSatisfied` com alerta sem `preco_alvo` (só `desconto_min`) e vice-versa — sem precisar de banco nem HTTP

---

## Sprint 5 — Crawler de Jogos (Nuuvem)

**Objetivo:** introduzir o padrão crawler com descoberta dinâmica de URLs. Fonte: Nuuvem.

**Sequência:** T5.1 (gate) → T5.2 → T5.3 integra → T5.4 testa.

---

- [ ] **T5.1 — Struct Crawler e pesquisa HTML** 🟡

  Com DevTools, mapear seletores da Nuuvem: nome, preço, desconto, link, paginação. Nuuvem embute um JSON por produto no atributo `data-default-tracker-product-tracking-data-param` de cada card — priorizar a leitura desse atributo (via `goquery` + `json.Unmarshal`) à extração por seletor de texto, que é mais frágil a redesigns visuais. Criar `internal/scraper/crawler.go` com a struct `Crawler` de `ARCHITECTURE.md §4.1`. Sem lógica de crawl ainda.

  *↳ Depende de: T1.2*
  **⚠️ Gate da sprint.**
  **Arquivos tocados:** `internal/scraper/crawler.go`, `internal/scraper/nuuvem/nuuvem.go` (vazio)

  **Conceitos a estudar:**
  - DevTools: Elements → inspecionar → Copy selector
  - `sync.Map`: por que em vez de `map` + `sync.Mutex` para URLs visitadas → [sync.Map docs](https://pkg.go.dev/sync#Map)
  - Channel buffered como fila: `make(chan string, 100)` — o que acontece quando está cheio

---

- [ ] **T5.2 — Crawler Nuuvem** 🔴

  `internal/scraper/nuuvem/nuuvem.go` implementando `Scraper` com padrão crawler. Seed: `https://www.nuuvem.com/br-pt/catalog/price/promo/sort/bestselling/sort-mode/desc`. Descobre paginação dinamicamente.

  *↳ Depende de: T5.1*
  **Arquivos tocados:** `internal/scraper/nuuvem/nuuvem.go`

  **Conceitos a estudar:**
  - `sync.Map`: `LoadOrStore` para verificar-e-marcar atomicamente
  - goquery: `.Find`, `.Each`, `.Text()`, `.Attr("href")`, `.Attr("data-...")` para extrair o JSON embutido
  - `url.ResolveReference`: transformar link relativo em URL absoluta

---

- [ ] **T5.3 — Integração no scheduler** 🟡

  Adicionar Nuuvem ao ticker de jogos (6h), junto com Steam. Falha na coleta não cancela as demais fontes.

  *↳ Depende de: T5.2, T1.5*
  **Arquivos tocados:** `internal/scheduler/scheduler.go`, `cmd/Umbra/main.go`

  **Conceitos a estudar:**
  - `[]Scraper`: iterar sobre todas as fontes, chamar `Collect` em cada uma
  - Loop com tratamento de erro individual por fonte

---

- [ ] **T5.4 — Testes da Sprint 5** 🔴

  Crawler Nuuvem com `httptest.NewServer` servindo HTML fixo. Validar: descobre paginação, não revisita URL, extrai dados corretos (incluindo parsing do JSON embutido no atributo `data-*`). `go test -race`.

  *↳ Depende de: T5.2*
  **Arquivos tocados:** `internal/scraper/nuuvem/nuuvem_test.go`

  **Conceitos a estudar:**
  - `httptest.NewServer` servindo múltiplas páginas com links entre si
  - `sync/atomic.Int64`: contar requisições para validar ausência de revisitas
  - `go test -race -count=5`

---

## Sprint 6 — Crawler Kabum (com worker pool)

**Objetivo:** aplicar padrão crawler da Sprint 5 no Kabum, adicionando worker pool e rate limiting.

**Sequência:** T6.1 (gate) → T6.2 → T6.3 → T6.4 → T6.5

---

- [ ] **T6.1 — Pesquisa HTML: Kabum** 🟢

  DevTools: mapear seletores de Kabum. Criar `internal/scraper/kabum/kabum.go` vazio com seletores documentados como comentário.

  *↳ Depende de: T5.1*
  **⚠️ Gate da sprint.**
  **Arquivos tocados:** `internal/scraper/kabum/kabum.go` (vazio com comentários)

  **Conceitos a estudar:**
  - Hardware tem dezenas de categorias e centenas de produtos — escala diferente de jogos
  - Estrutura de paginação do Kabum: `?page=N` ou `/page/N/` — verificar no site real

---

- [ ] **T6.2 — Crawler Kabum (sequencial)** 🔴

  Implementar `Scraper`. Seed: `https://www.kabum.com.br/hardware`. Um worker só por enquanto.

  *↳ Depende de: T6.1*
  **Arquivos tocados:** `internal/scraper/kabum/kabum.go`

  **Conceitos a estudar:**
  - Reusar struct `Crawler` da Sprint 5: só as funções `extract` e `links` mudam
  - `context.WithTimeout` por página: página lenta não trava a coleta inteira

---

- [ ] **T6.3 — Worker pool e rate limiting no Crawler** 🔴

  Refatorar `internal/scraper/crawler.go` para processar URLs com N goroutines paralelas. Delay mínimo de 2s entre requisições. Validar com `go test -race`.

  *↳ Depende de: T6.2*
  **Arquivos tocados:** `internal/scraper/crawler.go`

  **Conceitos a estudar:**
  - Worker pool: jobs channel buffered, N goroutines consumindo com `for url := range jobs`
  - `sync.WaitGroup`: `wg.Add(1)`, `defer wg.Done()`, `wg.Wait()`
  - `time.Sleep` dentro de goroutine: bloqueia só aquela goroutine, não o programa

---

- [ ] **T6.4 — Integração Kabum no scheduler** 🟡

  Adicionar Kabum ao ticker de hardware (12h).

  *↳ Depende de: T6.2, T6.3*
  **Arquivos tocados:** `internal/scheduler/scheduler.go`, `cmd/Umbra/main.go`

  **Conceitos a estudar:**
  - Dois tickers no mesmo `select`: como Go escolhe quando ambos estão prontos
  - `context.WithCancel` para encerrar ticker de hardware sem afetar o de jogos

---

- [ ] **T6.5 — Testes da Sprint 6** 🔴

  Kabum com HTML mockado. Worker pool: `go test -race -count=10`. Validar rate limiting e ausência de URLs revisitadas.

  *↳ Depende de: T6.2, T6.3*
  **Arquivos tocados:** `internal/scraper/kabum/kabum_test.go`, `internal/scraper/crawler_test.go`

  **Conceitos a estudar:**
  - `httptest.NewServer` com múltiplos paths: simular categorias e paginação
  - `sync/atomic.Int64`: contar requisições no mock
  - `go test -race -count=10`: múltiplas execuções aumentam chance de detectar race condition

---

## Sprint 7 — Crawler Pichau (consolidação)

**Objetivo:** replicar o padrão da Sprint 6 no Pichau. Worker pool já dominado — desafio é só o HTML diferente.

**Sequência:** T7.1 (gate) → T7.2 → T7.3 → T7.4

---

- [ ] **T7.1 — Pesquisa HTML: Pichau** 🟢

  DevTools: mapear seletores do Pichau. Criar `internal/scraper/pichau/pichau.go` vazio com seletores documentados.

  *↳ Depende de: T6.1*
  **⚠️ Gate da sprint.**
  **Arquivos tocados:** `internal/scraper/pichau/pichau.go` (vazio com comentários)

  **Conceitos a estudar:**
  - HTML do Pichau vs Kabum: quais diferenças estruturais existem
  - Como a mesma struct `Crawler` acomoda dois HTMLs via funções injetadas

---

- [ ] **T7.2 — Crawler Pichau** 🔴

  `internal/scraper/pichau/pichau.go` implementando `Scraper`. Seed: `https://www.pichau.com.br/hardware`. Worker pool já configurado via `Crawler` da Sprint 6.

  *↳ Depende de: T7.1, T6.3*
  **Arquivos tocados:** `internal/scraper/pichau/pichau.go`

  **Conceitos a estudar:**
  - `Crawler{extract: pichauExtract, links: pichauLinks}` — pool e rate limiting já vêm de graça
  - Diferenças de formatação de preço entre Pichau e Kabum

---

- [ ] **T7.3 — Integração Pichau no scheduler** 🟡

  Adicionar Pichau ao ticker de hardware. Garantir que falha do Pichau não cancela Kabum.

  *↳ Depende de: T7.2, T6.4*
  **Arquivos tocados:** `internal/scheduler/scheduler.go`, `cmd/Umbra/main.go`

  **Conceitos a estudar:**
  - `[]Scraper` no loop de hardware: iterar Kabum e Pichau independentemente
  - Error handling por fonte: `for _, s := range hwScrapers { if err != nil { slog.Error(...) } }`

---

- [ ] **T7.4 — Testes da Sprint 7** 🔴

  Pichau com HTML mockado. `go test -race -count=10`. Validar que Kabum e Pichau rodam no mesmo ticker sem interferência.

  *↳ Depende de: T7.2*
  **Arquivos tocados:** `internal/scraper/pichau/pichau_test.go`

  **Conceitos a estudar:**
  - Testar dois scrapers no mesmo ticker: verificar que ambos foram chamados
  - Comparar cobertura acumulada com metas de `ARCHITECTURE.md §10.2`

---

## Sprint 8 — Polimento e Entrega

**Objetivo:** projeto demonstrável. Todos os critérios de `REQUISITOS.md §8` verdes.

---

- [ ] **T8.1 — Auditoria de erros e logging** 🟡

  Substituir `fmt.Println` por `slog`. Toda falha de integração loga com contexto (fonte + produto + erro). Nenhum erro silenciado.

  *↳ Depende de: Sprint 7 completa*
  **Arquivos tocados:** todos os arquivos com `fmt.Println` para log

  **Conceitos a estudar:**
  - `slog`: níveis, campos estruturados, dev (texto legível) vs prod (JSON)
  - `errors.Join`: agregar múltiplos erros de uma iteração
  - Contexto suficiente no log: fonte + id do produto + erro original encadeado

---

- [ ] **T8.2 — Linter e cobertura** 🟢

  `golangci-lint run`, corrigir apontamentos. `go test -coverprofile` e verificar metas de `ARCHITECTURE.md §10.2`.

  *↳ Depende de: T8.1*
  **Arquivos tocados:** `.golangci.yml`, correções espalhadas

  **Conceitos a estudar:**
  - `.golangci.yml`: linters habilitados, como desabilitar um específico com justificativa
  - `go tool cover -html=cov.out`: visualizar cobertura por arquivo no browser

---

- [ ] **T8.3 — README e validação final** 🟢

  README: o que é, pré-requisitos, `.env` setup, `make` targets, exemplo de notificação Discord, como criar um alerta, arquitetura em 5 linhas. Validar todos os critérios de `REQUISITOS.md §8`.

  *↳ Depende de: T8.2*
  **Arquivos tocados:** `README.md`

---

*Task sem rastreabilidade em REQUISITOS.md ou ARCHITECTURE.md não entra no backlog.*
