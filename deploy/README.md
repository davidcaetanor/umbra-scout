# Deploy

Produção: `compose.yaml`, 

## `.env`, ao lado deste arquivo

| Variável                            | Obrigatória | Padrão      |
| :---------------------------------- | :---------- | :---------- |
| `UMBRA_DB_NAME`                     | sim         | nenhum      |
| `UMBRA_DB_USER`                     | sim         | nenhum      |
| `UMBRA_DB_PASSWORD`                 | sim         | nenhum      |
| `UMBRA_JWT_SECRET`                  | sim         | nenhum      |
| `UMBRA_COLETA_CONTATO`              | sim         | nenhum      |
| `ITAD_API_KEY`                      | sim         | nenhum      |
| `DISCORD_CLIENT_ID`                 | sim         | nenhum      |
| `DISCORD_CLIENT_SECRET`             | sim         | nenhum      |
| `GOOGLE_CLIENT_ID`                  | sim         | nenhum      |
| `GOOGLE_CLIENT_SECRET`              | sim         | nenhum      |
| `UMBRA_LOGIN_RETORNO_FRONT`         | sim         | nenhum      |
| `UMBRA_JWT_TTL_HORAS`               | não         | `6`         |
| `UMBRA_CORS_ORIGENS`                | não         | nenhuma     |
| `UMBRA_IMAGE_TAG`                   | não         | `latest`    |
| `UMBRA_COLETA_HABILITADA`           | não         | `false`     |
| `UMBRA_COLETA_AGENDA_RODADAINICIAL` | não         | `false`     |
| `UMBRA_WEB_API_URL`                 | sim (web)   | nenhum      |
| `UMBRA_WEB_LOGIN_GOOGLE`            | não         | `false`     |
| `UMBRA_WEB_IMAGE_TAG`               | não         | `latest`    |

Sem qualquer uma das obrigatórias a aplicação morre na partida. `UMBRA_JWT_SECRET` precisa de pelo menos 32 caracteres (256 bits). `UMBRA_COLETA_CONTATO` tem que ser um email válido: ele vai no `User-Agent` da coleta.

`UMBRA_CORS_ORIGENS` lista, separadas por vírgula, as origens do frontend autorizadas a chamar a API pelo navegador (`https://host` ou `https://host:porta`, sem curinga). Sem ela, nenhuma origem externa é aceita.

Login com Discord e Google: as credenciais vêm da aplicação registrada em cada provedor, que precisa aceitar como endereço de retorno `https://<host da API>/login/oauth2/code/discord` e `https://<host da API>/login/oauth2/code/google`. `UMBRA_LOGIN_RETORNO_FRONT` é a URL da tela do frontend que recebe `?codigo=` ou `?erro=` ao fim do login e troca o código pelo token em `POST /auth/oauth2/token`.

`UMBRA_DB_URL`, `SPRING_PROFILES_ACTIVE=prod` e `TZ` já vêm no compose.

Frontend (`web`): `UMBRA_WEB_API_URL` é a origem da API como o navegador a vê (`https://host`, sem caminho), e `UMBRA_WEB_LOGIN_GOOGLE` mostra ou oculta o botão do Google. As duas viram o `config.json` do front na partida do contêiner. O serviço recebe só essas duas variáveis, nunca o `.env` inteiro: tudo o que chega ao front é público. A origem do front precisa estar em `UMBRA_CORS_ORIGENS`, e `UMBRA_LOGIN_RETORNO_FRONT` aponta para `https://<host do front>/login/callback`. O contêiner roda sem root e com sistema de arquivos somente leitura.

Imagens (`imagens`): o front serve as capas em `/imagens/itad/` e `/imagens/kabum/` e as entrega ao imgproxy, que busca a imagem na origem e devolve em WebP. Ele aceita só o tamanho pré-definido `capa` e só as origens do ITAD e da Kabum, não publica porta e não lê o `.env`. O contêiner roda sem root e com sistema de arquivos somente leitura. Sem cache na frente, cada pedido de imagem que o navegador ainda não tem converte de novo; o navegador guarda a capa por 30 dias.

## HTTPS

A API escuta só em `127.0.0.1:8080` e não deve ser exposta em HTTP: login e token trafegam por ela. Na frente dela fica um proxy reverso com HTTPS no mesmo host, por exemplo o Caddy:

```
api.exemplo.com {
    reverse_proxy 127.0.0.1:8080
}

exemplo.com {
    reverse_proxy 127.0.0.1:8081
}
```

O perfil `prod` confia no `X-Forwarded-For` apenas quando a conexão vem do próprio host, então o IP do cliente usado nos limites por IP é o informado pelo proxy, e um cabeçalho enviado direto de fora é ignorado.

O projeto do compose se chama `umbra`; o volume do banco mantém o nome `deploy_umbra_pgdata`, de instalações anteriores ao nome explícito.

Permissão: `chmod 700` no diretório e `chmod 600` no `.env`.

## Subir

```bash
docker compose pull
docker compose up -d
docker compose ps
curl -fsS localhost:8080/actuator/health
```

## Atualizar

```bash
docker compose pull api
docker compose up -d api
```

## Rollback

`UMBRA_IMAGE_TAG=<sha anterior>` no `.env`, depois `up -d api`. As tags estão em `ghcr.io/davidcaetanor/umbra-api`.

## Logs

```bash
docker compose logs -f api
```

Rotação declarada no compose: 20 MB por arquivo, 5 arquivos por serviço. Para guardar uma janela inteira, extraia antes de ela rolar:

```bash
docker compose logs --no-color --timestamps api > umbra-api.log
```

## Obs Postgres

- **O volume monta `/var/lib/postgresql`, não `/var/lib/postgresql/data`.** A imagem 18 põe o `PGDATA` em `18/docker`; montar o caminho antigo faz o banco nascer vazio a cada recriação. Confira com `docker compose exec postgres printenv PGDATA`.
