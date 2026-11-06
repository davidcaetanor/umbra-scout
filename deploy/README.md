# Deploy

Produção: `docker-compose.prod.yaml` (o da raiz é desenvolvimento). Requer Docker com Compose v2.

## `.env`, ao lado deste arquivo

| Variável                            | Obrigatória | Padrão      |
| :---------------------------------- | :---------- | :---------- |
| `UMBRA_DB_NAME`                     | sim         | —           |
| `UMBRA_DB_USER`                     | sim         | —           |
| `UMBRA_DB_PASSWORD`                 | sim         | —           |
| `UMBRA_ITAD_API_KEY`                | sim         | —           |
| `UMBRA_IMAGE_TAG`                   | não         | `latest`    |
| `UMBRA_API_BIND`                    | não         | `127.0.0.1` |
| `UMBRA_COLETA_HABILITADA`           | não         | `false`     |
| `UMBRA_COLETA_AGENDA_RODADAINICIAL` | não         | `false`     |

`UMBRA_DB_URL`, `SPRING_PROFILES_ACTIVE=prod` e `TZ` já vêm no compose.

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

## Obs Postgres

- **O volume monta `/var/lib/postgresql`, não `/var/lib/postgresql/data`.** A imagem 18 põe o `PGDATA` em `18/docker`; montar o caminho antigo faz o banco nascer vazio a cada recriação. Confira com `docker compose -f docker-compose.prod.yaml exec db printenv PGDATA`.
