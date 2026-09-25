# syntax=docker/dockerfile:1
# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# baixa dependências numa camada própria (cache) antes de copiar o código
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package && cp target/ford-retention-ai-*.jar /app/app.jar

# ---------- runtime ----------
# Apenas JRE (sem JDK, Maven ou código-fonte) em Alpine: superfície de ataque menor
FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.title="ford-retention-ai" \
      org.opencontainers.image.description="API Ford Retention AI (Challenge FIAP 2026 - Desafio 02)"

# usuário sem privilégios, sem shell de login e com UID fixo
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app -s /sbin/nologin app
WORKDIR /app
COPY --from=build --chown=app:app /app/app.jar app.jar

USER 10001:10001
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom"
# Nenhum segredo na imagem: JWT_SECRET, FIELD_ENCRYPTION_KEY e DB_* são injetados em runtime
# (docker run --env-file / secrets do orquestrador / Azure Key Vault)
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:${MANAGEMENT_PORT:-9091}/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
