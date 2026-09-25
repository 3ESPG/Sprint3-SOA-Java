package br.com.fiap.fordretention.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuração do JWT. O segredo vem obrigatoriamente da variável de ambiente JWT_SECRET
 * (a aplicação não sobe sem ele) e precisa ter pelo menos 32 caracteres (256 bits, exigência do HS256).
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank(message = "defina a variável de ambiente JWT_SECRET")
        @Size(min = 32, message = "JWT_SECRET deve ter pelo menos 32 caracteres")
        String secret,

        @NotNull
        Duration expiration,

        @NotBlank
        String issuer
) {
}
