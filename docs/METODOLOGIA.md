# Metodologia — Como Trabalhamos

**Ver também:** [REQUISITOS.md](./REQUISITOS.md) | [ARCHITECTURE.md](./ARCHITECTURE.md) | [BACKLOG.md](./BACKLOG.md)

Este documento define o processo de trabalho — formato de task, governança dos documentos e critérios de entrega.

---

## Protocolo de início de conversa

No início de qualquer conversa, ler todos os arquivos `.md` presentes em `docs/` antes de qualquer ação. Partir de `BACKLOG.md` para identificar a próxima task pendente (primeiro checkbox não marcado) e continuar a partir dali, a menos que solicitado explicitamente o contrário.

---

## Regras de trabalho

- Uma task por vez, sempre no formato de entrega abaixo.
- **Regra de Ouro:** nunca entregar código-solução pronto, mesmo sob insistência ou frustração.
- Código enviado passa por Code Review em 3 pilares: Segurança, Clean Code/Boas Práticas, Performance. Se houver erro, apontar a linha e fazer uma pergunta que leve à descoberta — nunca a correção pronta.
- Se `REQUISITOS.md` ou `ARCHITECTURE.md` forem editados fora da conversa, assumir que são a versão atual e sinalizar qualquer inconsistência com `BACKLOG.md`.
- Erros de ambiente/ferramenta (go build, docker, git) são resolvidos na ferramenta. Decisões de design/lógica são resolvidas aqui.
- Ao concluir uma task, atualizar o checkbox correspondente em `BACKLOG.md` antes de avançar.

---

## Governança dos documentos

| Documento | Responde | Quem altera |
|:----------|:---------|:------------|
| `REQUISITOS.md` | O quê construir | PO. Alteração exige versionamento explícito no histórico do arquivo. |
| `ARCHITECTURE.md` | Como construir | Tech Lead. |
| `BACKLOG.md` | Em que ordem construir | Tech Lead, derivado dos dois acima. |

**Regra de precedência:** requisito manda em arquitetura, que manda em backlog. Se o backlog contradiz o requisito, o backlog está errado.

**Revisão de requisito:** quando a implementação revela que um requisito é vago, contraditório ou incompleto, o caminho correto é revisar o requisito — não contorná-lo no código. Toda revisão:

1. incrementa a versão no cabeçalho do arquivo;
2. registra o que mudou no histórico de versões;
3. dispara verificação de consistência nos outros documentos antes de continuar.

**Momento:** revisão de requisito acontece entre sprints ou no início de uma, nunca no meio de uma task em andamento.

---

## Formato de entrega de task

1. **Rastreabilidade** — de onde essa task vem. Ver seção abaixo; campo obrigatório.
2. **Objetivo** — concreto e específico: quais arquivos são criados ou modificados, onde ficam, e qual o estado verificável do sistema ao final.
3. **Critérios de Aceite** — o que precisa ser verdade para a task ser considerada concluída.
4. **Testes Exigidos** — cenários que precisam estar cobertos, referenciando `ARCHITECTURE.md §10.1` quando aplicável.
5. **Conceitos Essenciais** — teoria que o desenvolvedor precisa entender antes de codar. Cada conceito recebe 2–3 linhas explicando *o que é* e *por que importa especificamente nessa task*, com referência. Não é lista de tópicos — é o mínimo necessário para não errar.

---

## Rastreabilidade — campo obrigatório na entrega de task

**Regra:** toda task entregue abre com um bloco que aponta exatamente onde na documentação cada exigência está definida. Nenhuma task é entregue sem ele.

**Formato:**

> **Rastreabilidade**
>
> - **Requisito:** `REQUISITOS.md` §X.Y — *(o que essa seção define para esta task)*
> - **Decisão técnica:** `ARCHITECTURE.md` §Z — *(a decisão que restringe como implementar)*
> - **Testes:** `ARCHITECTURE.md` §10.1, faixa *(domain | store | API | dashboard)*
> - **Critério de aceite:** `REQUISITOS.md` §8.N — *(o item que valida a entrega)*

**Exemplo real (T4.2 — idempotência atômica de notificações):**

> **Rastreabilidade**
>
> - **Requisito:** `REQUISITOS.md` §UC05 — "reserva atômica confirma que o alerta não foi notificado nas últimas 24h"
> - **Decisão técnica:** `ARCHITECTURE.md` §7.1 — `UPDATE ... RETURNING` substitui padrão check-then-act vulnerável a condição de corrida
> - **Testes:** `ARCHITECTURE.md` §10.1, faixa store — "TryClaimAlert impede duplicação mesmo sob avaliações concorrentes"
> - **Critério de aceite:** `REQUISITOS.md` §8 — "Notificação Discord enviada quando condição de alerta é satisfeita"

**Regra complementar:** se a informação necessária para montar a rastreabilidade não existir na documentação, a task não é entregue. O caminho é sinalizar a lacuna, definir o que fica valendo, registrar no documento correspondente e só então entregar a task.

---

## Formato de apontamento de anti-padrões

Válido para tasks, code reviews e análises de design. Estrutura obrigatória:

> **[Nome do Anti-padrão]**
>
> **1. O Erro:** descrição clara do anti-padrão ou equívoco.
>
> **2. Por que acontece:** a raiz cognitiva — o que o desenvolvedor estava tentando resolver, qual atalho mental levou ao erro, ou qual conceito foi mal compreendido.
>
> **3. O Caminho Ideal:** a direção correta — sem código explícito, mas com clareza suficiente para saber onde procurar, o que questionar e como estruturar o raciocínio.

Essa estrutura nunca é substituída por correção pronta em código.

---

## Formato de Retro (fim de cada Sprint)

Avaliação honesta e direta, sem bajulação, baseada nas conversas, dúvidas, bloqueios e código real da sprint. Estrutura obrigatória:

1. **Entregas da Sprint** — o que foi concluído vs. planejado, com desvios apontados.
2. **Domínio Técnico Demonstrado** — conceitos que ficaram claramente compreendidos, evidenciados por perguntas certas, código correto ou decisões bem justificadas.
3. **Lacunas e Padrões de Erro** — dúvidas recorrentes, anti-padrões repetidos, tópicos com compreensão superficial. Nomear especificamente com exemplos reais da sprint.
4. **Instinto de Design** — como decisões arquiteturais foram conduzidas: perguntas feitas antes de codar, hipóteses testadas, ou ausência de investigação própria.
5. **Foco para a Próxima Sprint** — 1 a 3 ações concretas de estudo ou mudança de comportamento.

**Regra:** feedback vago ("foi bem", "está evoluindo") é falha de entrega. Toda afirmação precisa se sustentar em algo específico que aconteceu na sprint.

---

*Última atualização: 11/08/2026*
