# Deploy do Umbra

Passos para subir o Umbra do zero numa máquina limpa, com a aplicação e o Postgres no mesmo host por Docker Compose. O servidor de hoje é uma VPS Hostinger KVM 1 (1 vCPU, 4 GB de RAM) em São Paulo, com Ubuntu 26.04, Docker 29.9.3 e Compose v5.7.0.

O arquivo `docker-compose.prod.yaml` desta pasta é o de produção. O `docker-compose.yaml` da raiz é o de desenvolvimento e não serve aqui.

## Endurecimento do servidor

Já aplicado no servidor atual. Fica escrito para que reconstruir a máquina não dependa de memória.

1. **Usuário comum com `sudo`**, e toda a operação feita por ele, nunca como root.
2. **SSH sem root e sem senha**, por chave. A configuração mora em `/etc/ssh/sshd_config.d/01-umbra-hardening.conf`, com `PermitRootLogin no` e `PasswordAuthentication no`. O nome começa com `01` de propósito: o `sshd` fica com a primeira ocorrência de cada diretiva, e o `50-cloud-init.conf` do provisionamento traz `PasswordAuthentication yes`. Um arquivo que ordene depois dele não tem efeito. Confira com `sudo sshd -T | grep -E 'permitrootlogin|passwordauthentication'`, que deve mostrar `no` nas duas, e com a recusa de um login de root.
3. **`ufw` ligado com a 22 restrita ao IP residencial**, e nenhuma outra porta aberta por ele.
4. **Fuso de São Paulo** na máquina: `sudo timedatectl set-timezone America/Sao_Paulo`. O container da aplicação recebe `TZ=America/Sao_Paulo` pelo próprio compose, então o horário do log não depende disto. O fuso da máquina vale para o que roda fora do Docker.

**O `ufw` não protege a 8080.** O Docker publica porta escrevendo regra própria no `iptables`, e essa regra é avaliada antes das do `ufw`. Um `ports: "8080:8080"` fica alcançável de qualquer origem mesmo com o `ufw` negando. A restrição da 8080 ao IP residencial tem que ficar no firewall do painel da Hostinger, que filtra antes de o pacote chegar à máquina. Depois de abrir a porta, confira a partir de uma rede que não seja a residencial, como o 4G do celular, que a 8080 não responde.

## Variáveis do `.env`

O `.env` fica em `deploy/`, ao lado do compose, e é criado à mão no servidor. Não existe arquivo de exemplo versionado, por decisão de 19/10/2026: a lista abaixo cumpre esse papel. Crie o arquivo já com permissão 600, antes de escrever qualquer valor:

```bash
install -m 600 /dev/null .env
```

O `.env` precisa ter `UMBRA_DB_NAME`, `UMBRA_DB_USER` e `UMBRA_DB_PASSWORD`, que o Postgres usa para criar o banco na primeira subida e a aplicação usa para se conectar. Precisa também de `UMBRA_JWT_SECRET`, com pelo menos 32 bytes aleatórios, de `UMBRA_COLETA_CONTATO`, o email enviado no User-Agent da coleta, e de `ITAD_API_KEY`. `UMBRA_COLETA_HABILITADA` vale `true` para o servidor coletar sozinho.

Opcionais: `UMBRA_IMAGE_TAG`, que escolhe a versão da imagem e assume `latest` quando ausente, e `UMBRA_JWT_TTL_HORAS`, que assume 4 no perfil `prod`. As sobrescritas de agenda e de limiar do alarme listadas no `ARCHITECTURE.md §8` também entram aqui quando forem necessárias, porque o compose repassa o `.env` inteiro para a aplicação.

`UMBRA_DB_URL` não vai no `.env`: o compose monta a URL com o nome do serviço `postgres`, e esse valor vence qualquer um que esteja no arquivo. `SPRING_PROFILES_ACTIVE` e `TZ` também são fixados pelo compose.

## Imagem no GHCR

O CI publica a imagem em `ghcr.io/davidcaetanor/umbra-api` a cada push na `main`, depois do `verify` verde. Pull request só constrói e confere a imagem, sem publicar.

**Pacote novo no GHCR nasce privado**, mesmo com o repositório público. Depois da primeira publicação, mude a visibilidade do pacote `umbra-api` para pública nas configurações do pacote no GitHub. É isso que deixa o servidor baixar a imagem sem autenticar no registry. Confira de uma máquina sem login no GHCR com `docker pull ghcr.io/davidcaetanor/umbra-api:latest`.

## Primeira subida

```bash
git clone https://github.com/davidcaetanor/umbra-scout.git
cd umbra-scout/deploy
install -m 600 /dev/null .env
```

Escreva as variáveis no `.env`. Para a primeira subida, e só para ela, acrescente `UMBRA_COLETA_AGENDA_RODADAINICIAL=true`, porque o banco do servidor nasce vazio e esperar o primeiro horário do cron deixaria o catálogo vazio por até seis horas.

```bash
docker compose -f docker-compose.prod.yaml pull
docker compose -f docker-compose.prod.yaml up -d
docker compose -f docker-compose.prod.yaml ps
curl -s localhost:8080/actuator/health
```

Os dois serviços devem aparecer como `healthy`, e o `health` deve responder `{"status":"UP"}`. Quando a rodada inicial terminar no log, **remova `UMBRA_COLETA_AGENDA_RODADAINICIAL` do `.env`** e recrie a aplicação com `docker compose -f docker-compose.prod.yaml up -d`. O motivo está na seção "Política de reinício".

## Atualização

```bash
cd umbra-scout
git pull
cd deploy
docker compose -f docker-compose.prod.yaml pull
docker compose -f docker-compose.prod.yaml up -d
```

Os dois primeiros comandos fazem coisas diferentes, e confundi-los é o erro comum:

- **`git pull`** atualiza o repositório, ou seja, o próprio `docker-compose.prod.yaml` e este README. Não baixa imagem.
- **`docker compose pull`** baixa a **imagem** da aplicação do GHCR, na tag que o `.env` aponta. Não atualiza o repositório.

Uma mudança só de código precisa do `docker compose pull`. Uma mudança no compose precisa do `git pull`. Fazer os dois sempre é o caminho que não erra. O `up -d` recria só os containers cuja imagem ou configuração mudou.

## Rollback

O CI publica cada versão da `main` com duas tags: `latest` e o hash curto do commit. Para voltar a uma versão anterior, escreva no `.env` a tag do commit desejado, por exemplo `UMBRA_IMAGE_TAG=1a1865e`, e suba de novo:

```bash
docker compose -f docker-compose.prod.yaml pull
docker compose -f docker-compose.prod.yaml up -d
```

As tags disponíveis aparecem na página do pacote `umbra-api` no GitHub. Para voltar a acompanhar a `main`, remova a linha ou escreva `latest`.

O rollback troca só a aplicação. As migrations do Flyway já aplicadas continuam no banco, então voltar para uma versão anterior a uma migration nova só funciona se o schema novo for compatível com o código antigo.

## Volume do Postgres

O volume nomeado `umbra_pgdata` é montado em `/var/lib/postgresql`. Os valores foram lidos da própria imagem, e não de memória:

```bash
docker inspect postgres:18-alpine --format '{{range .Config.Env}}{{println .}}{{end}}{{json .Config.Volumes}}'
```

Em 06/11/2026, com `PG_VERSION=18.6`, a saída trouxe `PGDATA=/var/lib/postgresql/18/docker` e o volume declarado em `{"/var/lib/postgresql":{}}`.

A partir da linha 18, a imagem guarda os dados num subdiretório por versão (`18/docker`) e declara o volume no diretório pai. Montar o pai deixa o `PGDATA` dentro do volume nomeado e não cria volume anônimo. Também permite, numa troca de versão maior, ter o 18 e o 19 lado a lado no mesmo volume para o `pg_upgrade`. **O caminho errado é o antigo `/var/lib/postgresql/data`**, das versões até a 17: na 18 ele fica fora do `PGDATA`, e o banco nasce vazio a cada recriação do container.

A persistência foi provada antes do primeiro deploy, com este mesmo compose: uma tabela gravada sobreviveu a `down` e `up`. No servidor, repita a prova depois da primeira subida. **Nunca rode `down -v`**: o `-v` apaga o volume e o banco junto.

## Política de reinício

Os dois serviços usam `restart: unless-stopped`: voltam sozinhos depois de queda do processo ou reinício da máquina, e ficam parados se alguém os parou com `docker compose stop`.

Isso é seguro porque a rodada inicial fica desligada. Com ela desligada, a aplicação reiniciada só volta a esperar o próximo horário do cron, sem chamar as fontes. **Se `UMBRA_COLETA_AGENDA_RODADAINICIAL=true` ficar no `.env`, cada reinício dispara uma rodada completa contra o ITAD, a Steam e a Kabum.** Uma aplicação reiniciando em laço vira então uma sequência de rodadas contra APIs de terceiros, que consome a cota do ITAD e arrisca bloqueio do IP do servidor. Por isso a rodada inicial é removida do `.env` logo depois da primeira subida.

## Logs

O log da aplicação fica no driver padrão do Docker, sem rotação configurada, então nada é descartado com o tempo. Ele pertence ao container: **recriar o container apaga o log**, e isso inclui o `up -d` depois de um `pull` com imagem nova. Antes de qualquer atualização num período de observação, guarde o log:

```bash
docker compose -f docker-compose.prod.yaml logs --no-color api > umbra-api-$(date +%Y%m%d-%H%M).log
```
