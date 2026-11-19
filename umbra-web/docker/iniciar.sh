#!/bin/sh
set -eu

case "${UMBRA_WEB_API_URL:-}" in
  https://* | http://*) ;;
  *) echo "UMBRA_WEB_API_URL ausente ou sem http(s)://" >&2; exit 1 ;;
esac

case "${UMBRA_WEB_LOGIN_GOOGLE:-}" in
  true | false) ;;
  *) echo "UMBRA_WEB_LOGIN_GOOGLE precisa ser true ou false" >&2; exit 1 ;;
esac

case "$UMBRA_WEB_API_URL" in
  *'"'* | *'\'* | *' '*) echo "UMBRA_WEB_API_URL com caractere inválido" >&2; exit 1 ;;
esac

printf '{"apiUrl":"%s","loginGoogle":%s}\n' "$UMBRA_WEB_API_URL" "$UMBRA_WEB_LOGIN_GOOGLE" > /run/umbra/config.json

exec caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
