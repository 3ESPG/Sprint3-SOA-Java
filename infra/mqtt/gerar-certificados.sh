#!/usr/bin/env bash
# PKI de laboratório para o broker MQTT: CA própria, certificado do broker e um certificado por cliente.
# O CN do certificado do veículo é o VIN (use_identity_as_username → ACL por VIN).
# Chaves privadas NUNCA vão para o Git (infra/mqtt/certs/ está no .gitignore).
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p certs && cd certs
DIAS=365

# CA da frota Ford (em produção: Azure Key Vault / PKI gerenciada)
openssl req -x509 -newkey rsa:3072 -nodes -days "$DIAS" -sha256 \
  -keyout ford-iot-ca.key -out ford-iot-ca.crt -subj "/O=Ford Retention AI/CN=Ford IoT CA (laboratorio)"

emitir() { # $1 = nome do arquivo, $2 = CN, $3 = extensões opcionais
  openssl req -newkey rsa:2048 -nodes -keyout "$1.key" -out "$1.csr" -subj "/O=Ford Retention AI/CN=$2"
  openssl x509 -req -in "$1.csr" -CA ford-iot-ca.crt -CAkey ford-iot-ca.key -CAcreateserial \
    -days "$DIAS" -sha256 -out "$1.crt" ${3:+-extfile <(printf "%s" "$3")}
  rm "$1.csr"
}
emitir broker localhost "subjectAltName=DNS:localhost,IP:127.0.0.1"
emitir veiculo-9BFZZZ55LA0000001 9BFZZZ55LA0000001      # um veículo (CN = VIN)
emitir ingestor ingestor-service                          # serviço que só lê telemetria

# CA "intrusa": certificado válido criptograficamente, mas não assinado pela CA da Ford
openssl req -x509 -newkey rsa:2048 -nodes -days 30 -keyout intruso.key -out intruso.crt \
  -subj "/O=Atacante/CN=9BFZZZ55LA0000001"
chmod 600 *.key
echo "Certificados gerados em $(pwd)"
