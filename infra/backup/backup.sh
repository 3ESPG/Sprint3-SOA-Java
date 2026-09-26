#!/usr/bin/env bash
# Backup lógico cifrado do banco da API (perfil dev/H2).
#   uso: BACKUP_KEY=... ./backup.sh <jdbc-url> [pasta-destino]
# Produção (Oracle 19c): mesmo fluxo com Data Pump (expdp) no lugar do H2 Script + cópia para storage imutável.
set -euo pipefail
URL=${1:?informe a URL JDBC, ex.: jdbc:h2:file:/dados/db}
DEST=${2:-"$(dirname "$0")/backups"}
: "${BACKUP_KEY:?defina BACKUP_KEY (segredo do cofre, nunca no Git)}"
H2=${H2_JAR:-$(ls ~/.m2/repository/com/h2database/h2/*/h2-*.jar | grep -v sources | tail -1)}
mkdir -p "$DEST"; chmod 700 "$DEST"
NOME="fordretention-$(date +%Y%m%d-%H%M%S)"
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT

# 1) dump lógico consistente (os telefones continuam cifrados pela aplicação: AES-256-GCM)
java -cp "$H2" org.h2.tools.Script -url "$URL" -user sa -script "$TMP/dump.sql"
# 2) compacta e cifra o arquivo inteiro com AES-256 (chave derivada com PBKDF2, 200 mil iterações)
gzip -9 "$TMP/dump.sql"
openssl enc -aes-256-cbc -pbkdf2 -iter 200000 -salt -pass env:BACKUP_KEY \
  -in "$TMP/dump.sql.gz" -out "$DEST/$NOME.sql.gz.enc"
# 3) hash para verificar integridade antes de restaurar
( cd "$DEST" && shasum -a 256 "$NOME.sql.gz.enc" > "$NOME.sql.gz.enc.sha256" )
chmod 600 "$DEST/$NOME".*
echo "backup: $DEST/$NOME.sql.gz.enc ($(du -h "$DEST/$NOME.sql.gz.enc" | cut -f1))"
