#!/usr/bin/env bash
# Restauração testada: verifica o hash, decifra e carrega o backup num banco NOVO, depois confere contagens.
#   uso: BACKUP_KEY=... ./restore.sh <arquivo.sql.gz.enc> <jdbc-url-destino>
set -euo pipefail
ARQ=${1:?arquivo .sql.gz.enc}; URL=${2:?URL JDBC do banco de destino (vazio)}
: "${BACKUP_KEY:?defina BACKUP_KEY}"
H2=${H2_JAR:-$(ls ~/.m2/repository/com/h2database/h2/*/h2-*.jar | grep -v sources | tail -1)}
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
INICIO=$(date +%s)

( cd "$(dirname "$ARQ")" && shasum -a 256 -c "$(basename "$ARQ").sha256" )
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass env:BACKUP_KEY -in "$ARQ" -out "$TMP/dump.sql.gz"
gunzip "$TMP/dump.sql.gz"
java -cp "$H2" org.h2.tools.RunScript -url "$URL" -user sa -script "$TMP/dump.sql"

for t in concessionarias usuarios clientes veiculos servicos leads; do
  printf "%-16s %s linhas\n" "$t" "$(java -cp "$H2" org.h2.tools.Shell -url "$URL" -user sa -sql "SELECT COUNT(*) FROM $t" | sed -n 2p)"
done
echo "restauração concluída em $(( $(date +%s) - INICIO ))s"
