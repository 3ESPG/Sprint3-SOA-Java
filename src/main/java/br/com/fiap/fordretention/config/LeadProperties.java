package br.com.fiap.fordretention.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/** Regra de geração automática de leads a partir do perfil calculado pelo ML. */
@Validated
@ConfigurationProperties(prefix = "app.lead")
public record LeadProperties(
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        BigDecimal scoreLimite
) {
}
