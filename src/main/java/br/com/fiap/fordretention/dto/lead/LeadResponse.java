package br.com.fiap.fordretention.dto.lead;

import br.com.fiap.fordretention.model.enums.OrigemLead;
import br.com.fiap.fordretention.model.enums.PerfilCliente;
import br.com.fiap.fordretention.model.enums.Prioridade;
import br.com.fiap.fordretention.model.enums.StatusLead;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LeadResponse(
        Long id,
        Long clienteId,
        String clienteNome,
        String clienteTelefone,
        PerfilCliente perfilCliente,
        BigDecimal scoreRisco,
        Long veiculoId,
        String vin,
        String modelo,
        Integer ano,
        Long concessionariaId,
        String concessionariaNome,
        String motivo,
        Prioridade prioridade,
        StatusLead status,
        OrigemLead origem,
        String observacao,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
) {
}
