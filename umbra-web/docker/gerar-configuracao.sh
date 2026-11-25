#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
  echo "uso: gerar-configuracao.sh <index.html de origem> <diretorio de destino>" >&2
  exit 2
fi

ORIGEM="$1"
DESTINO="$2"
MARCADOR='<!-- configuracao -->'

if ! printf '%s' "${UMBRA_WEB_API_URL:-}" | grep -Eq '^https?://[A-Za-z0-9.-]+(:[0-9]{1,5})?$'; then
  echo "UMBRA_WEB_API_URL precisa ser só a origem, como https://api.exemplo.com" >&2
  exit 1
fi

case "${UMBRA_WEB_LOGIN_GOOGLE:-}" in
  true | false) ;;
  *) echo "UMBRA_WEB_LOGIN_GOOGLE precisa ser true ou false" >&2; exit 1 ;;
esac

if [ "$(grep -cF "$MARCADOR" "$ORIGEM")" -ne 1 ]; then
  echo "$ORIGEM precisa ter o marcador $MARCADOR uma única vez" >&2
  exit 1
fi

CONFIGURACAO=$(printf '{"apiUrl":"%s","loginGoogle":%s}' "$UMBRA_WEB_API_URL" "$UMBRA_WEB_LOGIN_GOOGLE")
BLOCO="<script id=\"configuracao\" type=\"application/json\">$CONFIGURACAO</script>"

printf '%s\n' "$CONFIGURACAO" > "$DESTINO/config.json"
sed "s|$MARCADOR|$BLOCO|" "$ORIGEM" > "$DESTINO/index.html"
