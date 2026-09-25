package br.com.fiap.fordretention.dto.cliente;

import br.com.fiap.fordretention.model.enums.PerfilCliente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record ClienteFiltro(
        @Schema(description = "Parte do nome (sem diferenciar maiúsculas)")
        String nome,

        @Schema(example = "ABANDONO")
        PerfilCliente perfil,

        @Schema(example = "0.7", description = "Score de risco mínimo (0..1)")
        @DecimalMin("0.0") @DecimalMax("1.0")
        BigDecimal scoreMin,

        @Schema(description = "Apenas ADMIN; para os demais perfis é sempre a concessionária do token")
        Long concessionariaId
) {
}
