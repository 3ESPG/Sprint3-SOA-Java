package br.com.fiap.fordretention.dto.lead;

import br.com.fiap.fordretention.model.enums.StatusLead;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeadStatusRequest(
        @Schema(example = "CONTATADO")
        @NotNull
        StatusLead status,

        @Schema(example = "Cliente atendeu e pediu retorno na sexta")
        @Size(max = 500)
        String observacao
) {
}
