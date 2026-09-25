package br.com.fiap.fordretention.dto.lead;

import br.com.fiap.fordretention.model.enums.OrigemLead;
import br.com.fiap.fordretention.model.enums.PerfilCliente;
import br.com.fiap.fordretention.model.enums.Prioridade;
import br.com.fiap.fordretention.model.enums.StatusLead;
import io.swagger.v3.oas.annotations.media.Schema;

public record LeadFiltro(
        @Schema(example = "ABERTO")
        StatusLead status,

        @Schema(example = "ABANDONO", description = "Perfil atual do cliente")
        PerfilCliente perfil,

        Prioridade prioridade,

        OrigemLead origem,

        Long clienteId,

        @Schema(description = "Apenas ADMIN; para os demais perfis é sempre a concessionária do token")
        Long concessionariaId
) {
}
