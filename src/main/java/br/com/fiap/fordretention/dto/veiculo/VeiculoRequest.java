package br.com.fiap.fordretention.dto.veiculo;

import br.com.fiap.fordretention.model.enums.StatusGarantia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record VeiculoRequest(
        @Schema(example = "9BFZH55L0K8123456", description = "VIN com 17 caracteres (sem I, O e Q)")
        @NotBlank
        @Pattern(regexp = "(?i)^[A-HJ-NPR-Z0-9]{17}$", message = "VIN deve ter 17 caracteres alfanuméricos (sem I, O e Q)")
        String vin,

        @Schema(example = "Ranger")
        @NotBlank @Size(max = 60)
        String modelo,

        @Schema(example = "2019")
        @NotNull @Min(1950)
        Integer ano,

        @Schema(example = "68000")
        @NotNull @PositiveOrZero
        Integer quilometragem,

        @Schema(example = "1")
        @NotNull
        Long clienteId,

        @Schema(example = "EXPIRADA")
        @NotNull
        StatusGarantia statusGarantia
) {
}
