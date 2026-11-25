# Contexto do build: raiz do repositório, porque o cliente é gerado de umbra-api/openapi.json.
FROM node:24.15-alpine AS build
WORKDIR /build/umbra-web

COPY umbra-web/package.json umbra-web/package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY umbra-api/openapi.json /build/umbra-api/openapi.json
COPY umbra-web/ ./
RUN npm run build

FROM caddy:2.10-alpine
RUN addgroup -S -g 10001 umbra && adduser -S -u 10001 -G umbra -H -s /sbin/nologin umbra \
    && mkdir -p /srv /data /config /run/umbra \
    && chown -R umbra:umbra /srv /data /config /run/umbra

COPY --from=build /build/umbra-web/dist/umbra-web/browser/ /srv/
RUN rm /srv/config.json
COPY umbra-web/docker/Caddyfile /etc/caddy/Caddyfile
COPY --chmod=0555 umbra-web/docker/gerar-configuracao.sh /usr/local/bin/gerar-configuracao.sh
COPY --chmod=0555 umbra-web/docker/iniciar.sh /usr/local/bin/iniciar.sh

ENV XDG_DATA_HOME=/data XDG_CONFIG_HOME=/config
EXPOSE 8080
USER umbra
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:8080/saude || exit 1
ENTRYPOINT ["/usr/local/bin/iniciar.sh"]
