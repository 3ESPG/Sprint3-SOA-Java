package br.com.fiap.fordretention.security.crypto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Chave AES-256 para dados pessoais em repouso, em Base64 (32 bytes). Vem obrigatoriamente da variável
 * de ambiente FIELD_ENCRYPTION_KEY (em produção, do cofre de segredos). Gere com: openssl rand -base64 32
 */
@Validated
@ConfigurationProperties(prefix = "app.crypto")
public record CriptografiaProperties(
        @NotBlank(message = "defina a variável de ambiente FIELD_ENCRYPTION_KEY (openssl rand -base64 32)")
        String fieldKey
) {
}
