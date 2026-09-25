# Segurança da API – Ford Retention AI (Cybersecurity, Sprint 3)

Controles de segurança aplicados diretamente no código e na infraestrutura da API Java/Spring Boot,
com o pipeline DevSecOps que os verifica a cada PR. Cada item tem **commit próprio** na branch `devsecops`.

## 1. Controles implementados

| Controle | Onde está | Norma | Teste automatizado |
|---|---|---|---|
| **Rate limiting** (Bucket4j): login 5/min por IP, 100/min por usuário, 60/min anônimo → **429** + `Retry-After` | `security/RateLimitFilter` | OWASP API4:2023, ASVS V2.2.1 | `RateLimitIT` |
| **JWT seguro**: HS256, segredo ≥ 256 bits só por `JWT_SECRET`, validade **15 min**, `jti`, emissor validado, token sem `exp` recusado | `security/JwtService`, `JwtProperties` | OWASP API2:2023, ASVS V3.5 | `JwtServiceTest`, `SegurancaIT` |
| **RBAC** por perfil (`ADMIN` = Ford, `GESTOR_CONCESSIONARIA`, `CONSULTOR`): regras de URL + `@PreAuthorize` | `config/SecurityConfig`, controllers | OWASP API5:2023, ASVS V4.1 | `SegurancaIT`, `*ControllerIT` |
| **Isolamento por concessionária** (BOLA): dados de outra concessionária → **404** | `service/EscopoAcessoService` | OWASP API1:2023, ASVS V4.2.1 | `IsolamentoConcessionariaIT` |
| **Validação de entrada**: Bean Validation em todos os DTOs (VIN, e-mail, telefone, tamanhos), corpo > 64 KB → **413**, só JPQL parametrizado | `dto/**`, `security/LimiteTamanhoCorpoFilter` | OWASP API8, ASVS V5.1 | `SegurancaIT` |
| **Criptografia em repouso**: telefone do cliente com **AES-256-GCM** (IV aleatório, tag de integridade), chave em `FIELD_ENCRYPTION_KEY` | `security/crypto/*`, `model/Cliente` | LGPD art. 46, ASVS V6.2 | `CriptografiaCampoTest`, `CriptografiaDadosPessoaisIT` |
| **Senhas** com BCrypt; contas de auto-cadastro nascem inativas | `SecurityConfig`, `UsuarioService` | ASVS V2.4 | `AuthControllerIT` |
| **Logs estruturados (JSON)** com `traceId` e eventos de segurança, sem dados pessoais | `config/TraceIdFilter`, `AuthService`, handlers | ASVS V7.1/V7.2 | execução manual (seção 3) |
| **Actuator mínimo**: só `health` e `prometheus`; resto → 403; porta interna em prod | `application*.yml`, `SecurityConfig` | ASVS V14.3 | `ActuatorIT` |
| **Container**: multi-stage, só JRE, usuário não-root (UID 10001), sem segredos na imagem | `Dockerfile`, `.dockerignore` | CIS Docker 4.1 | job `container` do pipeline |
| **Pipeline DevSecOps**: Gitleaks, Semgrep, Trivy (SCA, IaC, imagem), testes, security-gate | `.github/workflows/devsecops.yml` | OWASP SAMM | — |
| **Dependabot** (maven, actions, docker) | `.github/dependabot.yml` | OWASP A06:2021 | — |

Eventos de log: `auth.login_sucesso`, `auth.login_falha` (e-mail mascarado), `auth.sem_token`,
`auth.token_invalido`, `authz.acesso_negado`, `rate_limit.excedido`, `lead.status_alterado`,
`crypto.migracao_concluida`. Nunca são registrados senha, token, telefone ou dados de cliente.

## 2. Variáveis de ambiente de segurança

```bash
cp .env.example .env
# gere os dois segredos (nunca versionados):
echo "JWT_SECRET=$(openssl rand -base64 48)"           >> .env
echo "FIELD_ENCRYPTION_KEY=$(openssl rand -base64 32)" >> .env
set -a; source .env; set +a
```

A aplicação **não sobe** sem `JWT_SECRET` (≥ 32 caracteres) e `FIELD_ENCRYPTION_KEY` (32 bytes em Base64).
Trocar a `FIELD_ENCRYPTION_KEY` torna ilegíveis os telefones já cifrados.

> **Oracle (perfil prod):** o telefone cifrado ocupa ~60 caracteres. O `ddl-auto=update` não aumenta
> colunas existentes, então rode uma vez: `ALTER TABLE clientes MODIFY telefone VARCHAR2(100);`

## 3. Como testar cada controle (e gerar as evidências)

Suba a API com logs em JSON:

```bash
set -a; source .env; set +a
LOGGING_STRUCTURED_FORMAT_CONSOLE=logstash mvn spring-boot:run
```

Em outro terminal (usuários do `data.sql`, senha `Ford@2026`):

```bash
API=http://localhost:8080
login() { curl -s -X POST $API/auth/login -H 'Content-Type: application/json' \
            -d "{\"email\":\"$1\",\"senha\":\"Ford@2026\"}" | jq -r .accessToken; }
ADMIN=$(login admin@ford.com)
GESTOR=$(login gestor.sp@fordcentral.com.br)
CONSULTOR=$(login consultor.sp@fordcentral.com.br)

# 401 – sem token / token inválido
curl -i $API/leads
INVALIDO=abc.def.ghi
curl -i $API/leads -H "Authorization: Bearer $INVALIDO"

# 403 – consultor em rota exclusiva da Ford (ADMIN)
curl -i -X POST $API/concessionarias -H "Authorization: Bearer $CONSULTOR" \
     -H 'Content-Type: application/json' -d '{}'

# 404 – consultor de SP pedindo lead de outra concessionária (isolamento)
curl -i $API/leads/<id-de-lead-do-RJ> -H "Authorization: Bearer $CONSULTOR"

# 429 – força bruta no login (espere 1 min após os logins acima)
for i in $(seq 1 6); do
  curl -s -o /dev/null -w "tentativa $i → %{http_code}\n" -X POST $API/auth/login \
       -H 'Content-Type: application/json' -d '{"email":"admin@ford.com","senha":"errada1"}'
done

# 413 – corpo acima de 64 KB
python3 -c "print('{\"email\":\"' + 'a'*70000 + '@x.com\",\"senha\":\"x\"}')" > /tmp/grande.json
curl -i -X POST $API/auth/login -H 'Content-Type: application/json' --data @/tmp/grande.json

# Métricas
curl -s $API/actuator/prometheus | grep http_server_requests_seconds_count | head
```

**Telefone cifrado no banco:** o console do H2 não é exposto. Use o teste `CriptografiaDadosPessoaisIT`,
que lê a coluna crua via SQL e confirma o prefixo `v1:`, ou, no Oracle, `SELECT nome, telefone FROM clientes;`.

**Testes e cobertura:** `mvn verify` → relatório em `target/site/jacoco/index.html` e
`target/surefire-reports/`.

**Docker (onde houver Docker instalado):**

```bash
docker build -t ford-retention-ai .
docker inspect --format '{{.Config.User}}' ford-retention-ai   # → 10001:10001
docker run --rm aquasec/trivy:0.74.0 image ford-retention-ai    # ou veja o job "container" no Actions
```

## 4. Achados do pipeline e tratamento

Primeira execução do `devsecops` (PR #1) — o pipeline encontrou problemas reais, corrigidos no mesmo PR:

| Job | Achado | Severidade | Tratamento |
|---|---|---|---|
| `container` (Trivy image) | `tomcat-embed-core` 10.1.55: CVE-2026-65182, CVE-2026-65905, CVE-2026-68525 | CRITICAL | `tomcat.version` = 10.1.59 no `pom.xml` |
| `container` (Trivy image) | `libexpat` 2.8.4 na base Alpine: CVE-2026-93990 | HIGH | `apk upgrade --no-cache` no estágio de runtime |
| `sca` (Trivy fs) | Maven Central respondeu 429 ao Trivy (sem cache de dependências) | — (falha do job) | `mvn dependency:go-offline` com cache + `--offline-scan` |
| `sast` (Semgrep) | `detected-bcrypt-hash` nos `data.sql` (usuários de demonstração) | Baixa | **Risco aceito**: senha de teste já pública; seed só no dev ou com `DB_SEED=always`. Arquivos em `.semgrepignore` com justificativa |
| `secrets` (Gitleaks) | `curl-auth-header` no exemplo com token inválido do README-SECURITY | Falso positivo | Exemplo passou a usar variável; fingerprint do commit antigo em `.gitleaksignore` |

Regra: nenhum achado é suprimido sem justificativa escrita no próprio arquivo de exceção.

## 5. Evidências para o documento de Cybersecurity

| # | Evidência | Como gerar |
|---|---|---|
| E1 | Pipeline rodando | Aba **Actions** → workflow `devsecops` (todos os jobs + `security-gate`) |
| E2 | Gitleaks bloqueando segredo | Branch de teste com um segredo falso → PR → job `secrets` vermelho → apagar a branch |
| E3 | Semgrep / Trivy | Logs dos jobs `sast`, `sca`, `iac` e `container` |
| E4 | Branch protection | Settings → Branches → regra na `main` exigindo PR e o check `security-gate` |
| E5 | Dependabot | Settings → Code security → Dependabot alerts/updates ativado ou PR aberto por ele |
| E6 | 401 / 403 | Comandos da seção 3 (terminal ou Postman) |
| E7 | 429 | Loop de 6 logins da seção 3 |
| E8 | JWT | Token de teste no jwt.io mostrando `exp` (15 min), `jti`, `role`, `concessionariaId` |
| E9 | Isolamento / RBAC | `mvn test -Dtest=IsolamentoConcessionariaIT,SegurancaIT` passando |
| E10 | Telefone cifrado | `mvn test -Dtest=CriptografiaDadosPessoaisIT,CriptografiaCampoTest` passando |
| E11 | Logs JSON | Terminal da API com os eventos da seção 3 |
| E12 | Container não-root | Passo "Imagem roda como usuário não-root" e Trivy no job `container` |
| E13 | Commits | `git log --oneline devsecops` (um commit por controle) |
