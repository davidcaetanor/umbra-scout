# Requisitos — Umbra

**Ver também:** [ARCHITECTURE.md](./ARCHITECTURE.md) | [BACKLOG.md](./BACKLOG.md) | [METODOLOGIA.md](./METODOLOGIA.md)

**Versão:** 1.7
**Data:** 10/08/2026

---

## Histórico de versões

| Versão | Data       | O que mudou |
| :----- | :--------- | :---------- |
| 1.0    | 10/08/2026 | Versão inicial |
| 1.1    | 10/08/2026 | Adicionado Nuuvem e gg.deals; scraping passa a usar padrão crawler com descoberta dinâmica de URLs |
| 1.2    | 10/08/2026 | Dashboard (UC06) e alertas personalizados (UC07) promovidos do Icebox; audit log (`log_evento`) adicionado; canal Discord compartilhado definido como modelo de notificação; Kabum passa a Sprint 6, Pichau a Sprint 7 |
| 1.3    | 10/08/2026 | gg.deals removido do MVP; nota sobre modelo de dados corrigida |
| 1.4    | 10/08/2026 | Persistência atualizada de pgx puro para sqlc sobre pgx |
| 1.5    | 10/08/2026 | Valores monetários migrados de `float64` para `int64` centavos |
| 1.6    | 10/08/2026 | Camada `internal/usecase/` adotada; regra de dependência atualizada |
| 1.7    | 10/08/2026 | `usuario_id` adicionado a `log_evento` e `alerta` (NULL até auth existir) |

---

## 1. Visão geral

O **Umbra** é um serviço que monitora preços de jogos e hardware, agrega promoções de múltiplas fontes e notifica via Discord quando um produto monitorado atinge o preço ou desconto desejado.

**Notificação:** canal Discord compartilhado via webhook único. Modelo preparado para adicionar `usuario_id` quando autenticação for implementada.

---

## 2. Objetivos de aprendizado

- [ ] Sintaxe e tipos básicos de Go (structs, interfaces, slices, maps, error handling)
- [ ] Módulos Go (go.mod, organização de pacotes)
- [ ] Separação de camadas (Clean Architecture simplificada): domain, usecase, store/notifier, delivery (api/dashboard/scheduler) — ver `ARCHITECTURE.md §2.1`
- [ ] HTTP client e consumo de APIs REST em Go
- [ ] Parsing de HTML com goquery
- [ ] Persistência com PostgreSQL via sqlc sobre pgx; CRUD completo; upsert; JSONB
- [ ] Concorrência real: goroutines, channels, worker pool
- [ ] Padrão de crawler: descoberta dinâmica de URLs, controle de URLs visitadas com sync.Map
- [ ] Agendamento de tarefas com time.Ticker
- [ ] Integração com webhook do Discord
- [ ] HTTP server com endpoints REST e server-side rendering com html/template
- [ ] Audit log: registros imutáveis de eventos de sistema
- [ ] Testes unitários e de integração em Go
- [ ] Docker multi-stage e docker-compose
- [ ] CI com GitHub Actions

---

## 3. Fontes de dados

| Fonte               | Tipo          | O que fornece                             | Fase     |
| :------------------ | :------------ | :---------------------------------------- | :------- |
| Steam API (oficial) | API REST/JSON | Jogos em promoção, preços, detalhes       | Sprint 1 |
| Nuuvem              | Crawler HTML  | Loja brasileira de jogos, promoções em BRL | Sprint 5 |
| Kabum               | Crawler HTML  | Hardware: GPU, CPU, consoles, periféricos | Sprint 6 |
| Pichau              | Crawler HTML  | Hardware: alternativa ao Kabum            | Sprint 7 |

**Padrão de coleta:** Steam usa API oficial (JSON). Nuuvem, Kabum e Pichau usam crawler com descoberta dinâmica de URLs — parte de URL semente, descobre links de categoria/paginação e os enfileira.

**gg.deals:** removido do MVP — bloqueado por Cloudflare challenge, incompatível com `net/http`+`goquery`. Ver `ARCHITECTURE.md §13`.

---

## 4. Casos de uso

### UC01 — Monitorar promoções de jogos (Steam)

**Quem usa:** scheduler
**Periodicidade:** a cada 6h (configurável)

1. Scheduler dispara a coleta
2. Sistema busca promoções na Steam API
3. Persiste o preço atual com timestamp; registra evento em `log_evento`
4. Para cada produto com alerta ativo: verifica condição → dispara Discord se necessário

**Regras:**

- Desconto mínimo de armazenamento configurável (padrão 5%) — filtra ruído, não controla notificações
- Notificações controladas pelos alertas individuais da tabela `alerta`
- Não renotificar o mesmo alerta nas próximas 24h
- Histórico append-only — nenhum registro de preço é sobrescrito

---

### UC02 — Monitorar preços de hardware (Kabum / Pichau)

**Quem usa:** scheduler
**Periodicidade:** a cada 12h (configurável)

1. Scheduler dispara coleta de hardware
2. Worker pool raspa páginas de categoria
3. Persiste preço atual de cada produto; registra evento em `log_evento`
4. Para cada produto com alerta ativo: verifica condição → dispara Discord se necessário

**Regras:**

- Worker pool com limite configurável de goroutines (padrão: 5)
- Delay mínimo entre requisições para o mesmo domínio (padrão: 2s)
- Produto identificado pelo par (fonte, identificador_interno)

---

### UC03 — Consultar histórico de preços

**Quem usa:** usuário via endpoint REST ou dashboard

1. Usuário acessa `GET /produtos/{id}/historico` ou `/dashboard/produtos/{id}`
2. Sistema retorna série temporal com preço mínimo histórico, preço atual, variação percentual

---

### UC04 — Listar promoções ativas

**Quem usa:** usuário via endpoint REST ou dashboard

1. Usuário acessa `GET /promocoes` ou `/dashboard`
2. Sistema retorna produtos com desconto ativo, ordenados por percentual
3. Filtros: `tipo` (jogo/hardware), `desconto_min`, `categoria`

---

### UC05 — Notificação via Discord

**Quem usa:** scheduler (acionado quando condição de alerta é satisfeita)
**Modelo:** webhook único → canal Discord compartilhado

1. Sistema avalia condição via `domain.AlertSatisfied` — função pura, sem I/O (ver `ARCHITECTURE.md §3.1`)
2. Reserva atômica no banco confirma que o alerta não foi notificado nas últimas 24h (ver `ARCHITECTURE.md §7.1`)
3. Se reserva bem-sucedida: monta e envia mensagem
4. Mensagem contém: nome do produto, fonte, preço atual, preço original, desconto, condição que disparou, link
5. Registra em `notificacao` e `log_evento`

---

### UC06 — Dashboard web

**Quem usa:** usuário via browser

1. `/dashboard` — promoções ativas com filtros
2. `/dashboard/produtos/{id}` — histórico de preços com gráfico
3. `/dashboard/alertas` — lista alertas ativos; cria e deleta
4. `/dashboard/log` — feed de eventos recentes do sistema

**Tecnologia:** `html/template` (stdlib Go), server-side rendering.

---

### UC07 — Gerenciar alertas personalizados

**Quem usa:** usuário via dashboard ou API REST

1. Usuário encontra um produto no dashboard
2. Preenche `preco_alvo` e/ou `desconto_min`
3. Sistema cria registro em `alerta`; registra evento em `log_evento`
4. A partir da próxima coleta, o scheduler verifica esse alerta

**Endpoints:**

- `POST /alertas` — cria alerta
- `GET /alertas` — lista alertas ativos
- `DELETE /alertas/{id}` — remove alerta

**Regras:**

- Produto precisa existir na tabela `produto`
- Pelo menos um dos campos (`preco_alvo` ou `desconto_min`) deve ser preenchido
- Deleção registra evento em `log_evento`

---

## 5. Regras de negócio globais

- Nenhum registro de preço é deletado — histórico append-only
- `log_evento` é append-only — nenhum evento é editado ou deletado
- Produto sem alteração de preço desde a última coleta ainda é registrado
- Falha em uma fonte não interrompe a coleta das demais
- Toda falha de integração externa é logada com contexto suficiente para diagnóstico

---

## 6. Fora do escopo do MVP

- Autenticação de usuários (`usuario_id` fica NULL)
- Múltiplos webhooks / canais Discord por usuário
- Busca por produto específico
- Preços internacionais (apenas BRL)
- Notificações por email ou Telegram
- gg.deals como fonte de dados
- GORM ou ORM tradicional
- `shopspring/decimal`

---

## 7. Modelo de dados

```
produto
  id              SERIAL PK
  nome            TEXT NOT NULL
  fonte           TEXT NOT NULL          -- 'steam', 'kabum', 'pichau', 'nuuvem'
  identificador   TEXT NOT NULL
  categoria       TEXT                   -- 'jogo', 'gpu', 'cpu', 'console', ...
  url             TEXT NOT NULL
  criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
  UNIQUE(fonte, identificador)

preco
  id              SERIAL PK
  produto_id      INT FK → produto
  preco_brl       NUMERIC(10,2) NOT NULL
  preco_original  NUMERIC(10,2)
  desconto_pct    INT
  coletado_em     TIMESTAMPTZ NOT NULL DEFAULT now()

alerta
  id                     SERIAL PK
  produto_id             INT FK → produto
  preco_alvo             NUMERIC(10,2)
  desconto_min           INT
  usuario_id             INT NULL
  ultima_notificacao_em  TIMESTAMPTZ NULL       -- controla idempotência; ver ARCHITECTURE.md §7.1
  criado_em              TIMESTAMPTZ NOT NULL DEFAULT now()
  CHECK (preco_alvo IS NOT NULL OR desconto_min IS NOT NULL)

notificacao
  id              SERIAL PK
  produto_id      INT FK → produto
  alerta_id       INT FK → alerta
  preco_id        INT FK → preco
  canal           TEXT NOT NULL          -- 'discord'
  enviada_em      TIMESTAMPTZ NOT NULL DEFAULT now()

log_evento
  id              SERIAL PK
  entidade        TEXT NOT NULL          -- 'alerta', 'preco', 'notificacao', 'produto'
  entidade_id     INT NOT NULL
  acao            TEXT NOT NULL          -- 'criado', 'deletado'
  payload         JSONB
  usuario_id      INT NULL
  criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
```

---

## 8. Critérios de aceite do projeto

- [ ] `docker compose up` sobe o sistema completo sem configuração manual além do `.env`
- [ ] CI passa em todo PR: build, testes e linter
- [ ] Coleta da Steam roda automaticamente a cada 6h e persiste os dados
- [ ] Coleta de hardware roda automaticamente a cada 12h com worker pool
- [ ] Notificação Discord enviada quando condição de alerta é satisfeita
- [ ] `GET /promocoes` retorna promoções ativas com filtros funcionando
- [ ] `GET /produtos/{id}/historico` retorna série temporal correta
- [ ] `POST /alertas` cria alerta; `DELETE /alertas/{id}` remove; ambos registram em `log_evento`
- [ ] Dashboard `/dashboard` mostra promoções ativas
- [ ] Dashboard `/dashboard/produtos/{id}` mostra gráfico de histórico de preços
- [ ] Dashboard `/dashboard/alertas` permite criar e deletar alertas
- [ ] Dashboard `/dashboard/log` mostra feed de eventos recentes
- [ ] Toda criação e deleção de alerta, envio de notificação e inserção de preço registra em `log_evento`
- [ ] Nenhuma credencial versionada no repositório
- [ ] README com instruções de setup e uso

---

_REQUISITOS.md é fonte de verdade. Qualquer alteração deve incrementar a versão no cabeçalho e registrar o que mudou no histórico._
