#!/bin/sh
set -u

SCRIPT="$(dirname "$0")/gerar-configuracao.sh"
TRABALHO=$(mktemp -d)
trap 'rm -rf "$TRABALHO"' EXIT
FALHAS=0

printf '<html><head>\n<!-- configuracao -->\n</head></html>\n' > "$TRABALHO/index.html"

gerar() {
  rm -rf "$TRABALHO/saida" && mkdir "$TRABALHO/saida"
  UMBRA_WEB_API_URL="$1" UMBRA_WEB_LOGIN_GOOGLE="$2" sh "$SCRIPT" "$TRABALHO/index.html" "$TRABALHO/saida" 2>/dev/null
}

conferir() {
  if [ "$2" -eq 0 ]; then echo "ok    $1"; else echo "FALHA $1"; FALHAS=$((FALHAS + 1)); fi
}

gerar 'https://api.exemplo.com' true
conferir 'origem valida gera o bloco no index' \
  "$(grep -cF '<script id="configuracao" type="application/json">{"apiUrl":"https://api.exemplo.com","loginGoogle":true}</script>' "$TRABALHO/saida/index.html" | grep -qx 1; echo $?)"
conferir 'origem valida gera o config.json igual ao bloco' \
  "$(grep -qxF '{"apiUrl":"https://api.exemplo.com","loginGoogle":true}' "$TRABALHO/saida/config.json"; echo $?)"
conferir 'marcador some do index gerado' "$(grep -qF '<!-- configuracao -->' "$TRABALHO/saida/index.html"; [ $? -eq 1 ]; echo $?)"

gerar 'http://localhost:8080' false
conferir 'origem local com porta aceita' "$?"

for invalida in '' 'api.exemplo.com' 'ftp://api.exemplo.com' 'https://api.exemplo.com/' 'https://api.exemplo.com/caminho' \
  'https://api.exemplo.com?x=1' 'https://a.com"</script><script>alert(1)</script>' 'https://a.com&b' 'https://a.com|b' 'https://a .com'; do
  gerar "$invalida" true
  conferir "origem recusada: $invalida" "$([ $? -eq 1 ]; echo $?)"
  conferir "nada gerado para: $invalida" "$([ ! -e "$TRABALHO/saida/index.html" ]; echo $?)"
done

for invalido in '' 'TRUE' 'sim' '1'; do
  gerar 'https://api.exemplo.com' "$invalido"
  conferir "login google recusado: '$invalido'" "$([ $? -eq 1 ]; echo $?)"
done

printf '<html></html>\n' > "$TRABALHO/sem-marcador.html"
mkdir -p "$TRABALHO/saida2"
UMBRA_WEB_API_URL='https://api.exemplo.com' UMBRA_WEB_LOGIN_GOOGLE=true sh "$SCRIPT" "$TRABALHO/sem-marcador.html" "$TRABALHO/saida2" 2>/dev/null
conferir 'index sem marcador recusado' "$([ $? -eq 1 ]; echo $?)"

[ "$FALHAS" -eq 0 ]
