#!/bin/sh
set -eu

/usr/local/bin/gerar-configuracao.sh /srv/index.html /run/umbra

exec caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
