package br.com.fiap.fordretention.security;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Limites de requisições por minuto (OWASP API4:2023 – Unrestricted Resource Consumption).
 *
 * @param habilitado    liga/desliga o filtro (ligado por padrão)
 * @param loginPorMinuto tentativas de POST /auth/login por IP (freia força bruta de senha)
 * @param autenticadoPorMinuto requisições por usuário autenticado (chave = id do token)
 * @param anonimoPorMinuto demais requisições sem token, por IP
 */
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean habilitado,
        @Min(1) int loginPorMinuto,
        @Min(1) int autenticadoPorMinuto,
        @Min(1) int anonimoPorMinuto
) {
}
