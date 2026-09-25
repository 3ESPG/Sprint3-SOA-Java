package br.com.fiap.fordretention.dto.veiculo;

import br.com.fiap.fordretention.model.enums.StatusGarantia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record VeiculoFiltro(
        @Schema(example = "Ranger", description = "Parte do modelo (sem diferenciar maiúsculas)")
        String modelo,

        @Schema(example = "4", description = "Idade mínima do veículo em anos")
        @Min(0)
        Integer idadeMin,

        @Schema(description = "Idade máxima do veículo em anos")
        @Min(0)
        Integer idadeMax,

        Long clienteId,

        StatusGarantia statusGarantia,

        @Schema(description = "Apenas ADMIN; para os demais perfis é sempre a concessionária do token")
        Long concessionariaId
) {
}
