# Deploy

Produção: `docker-compose.prod.yaml` (o da raiz é desenvolvimento). Requer Docker com Compose v2.

## `.env`, ao lado deste arquivo

| Variável                            | Obrigatória | Padrão      |
| :---------------------------------- | :---------- | :---------- |
| `UMBRA_DB_NAME`                     | sim         | —           |
| `UMBRA_DB_USER`                     | sim         | —           |
| `UMBRA_DB_PASSWORD`                 | sim         | —           |
| `UMBRA_JWT_SECRET`                  | sim         | —           |
| `UMBRA_COLETA_CONTATO`              | sim         | —           |
| `ITAD_API_KEY`                      | sim         | —           |
| `UMBRA_JWT_TTL_HORAS`               | não         | `4`         |
| `UMBRA_IMAGE_TAG`                   | não         | `latest`    |
| `UMBRA_API_BIND`                    | não         | `127.0.0.1` |
| `UMBRA_COLETA_HABILITADA`           | não         | `false`     |
| `UMBRA_COLETA_AGENDA_RODADAINICIAL` | não         | `false`     |

Sem qualquer uma das obrigatórias a aplicação morre na partida. `UMBRA_COLETA_CONTATO` tem que ser um email válido: ele vai no `User-Agent` da coleta.

`UMBRA_DB_URL`, `SPRING_PROFILES_ACTIVE=prod` e `TZ` já vêm no compose.

Permissão: `chmod 700` no diretório e `chmod 600` no `.env`.

## Subir

```bash
docker compose -f docker-compose.prod.yaml pull
docker compose -f docker-compose.prod.yaml up -d
docker compose -f docker-compose.prod.yaml ps
curl -fsS localhost:8080/actuator/health
```

## Atualizar

```bash
docker compose -f docker-compose.prod.yaml pull api
docker compose -f docker-compose.prod.yaml up -d api
```

## Rollback

`UMBRA_IMAGE_TAG=<sha anterior>` no `.env`, depois `up -d api`. As tags estão em `ghcr.io/davidcaetanor/umbra-api`.

## Logs

```bash
docker compose -f docker-compose.prod.yaml logs -f api
```

Rotação declarada no compose: 20 MB por arquivo, 5 arquivos por serviço. Para guardar uma janela inteira, extraia antes de ela rolar:

```bash
docker compose -f docker-compose.prod.yaml logs --no-color --timestamps api > umbra-api.log
```

## Obs Postgres

- **O volume monta `/var/lib/postgresql`, não `/var/lib/postgresql/data`.** A imagem 18 põe o `PGDATA` em `18/docker`; montar o caminho antigo faz o banco nascer vazio a cada recriação. Confira com `docker compose -f docker-compose.prod.yaml exec postgres printenv PGDATA`.
