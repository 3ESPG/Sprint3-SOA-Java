package br.com.fiap.fordretention.dto.lead;

import br.com.fiap.fordretention.model.enums.Prioridade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Criação/substituição manual de lead. A concessionária responsável é a preferida do cliente. */
public record LeadRequest(
        @Schema(example = "1")
        @NotNull
        Long clienteId,

        @Schema(example = "1")
        @NotNull
        Long veiculoId,

        @Schema(example = "Revisão de 60.000 km vencida; oferecer pacote com desconto")
        @NotBlank @Size(max = 500)
        String motivo,

        @Schema(example = "ALTA")
        @NotNull
        Prioridade prioridade
) {
}
