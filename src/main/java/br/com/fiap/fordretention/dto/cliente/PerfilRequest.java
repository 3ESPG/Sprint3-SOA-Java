package br.com.fiap.fordretention.dto.cliente;

import br.com.fiap.fordretention.model.enums.PerfilCliente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Payload enviado pelo serviço de ML (notebook Python / Random Forest). */
public record PerfilRequest(
        @Schema(example = "ABANDONO")
        @NotNull
        PerfilCliente perfil,

        @Schema(example = "0.87", description = "Probabilidade de abandono da rede (0..1)")
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") @Digits(integer = 1, fraction = 4)
        BigDecimal scoreRisco
) {
}
