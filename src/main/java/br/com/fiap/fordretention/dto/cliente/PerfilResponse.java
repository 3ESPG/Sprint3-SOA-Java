package br.com.fiap.fordretention.dto.cliente;

import br.com.fiap.fordretention.model.enums.PerfilCliente;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PerfilResponse(
        Long clienteId,
        String clienteNome,
        PerfilCliente perfil,
        String perfilNome,
        String perfilDescricao,
        BigDecimal scoreRisco,
        LocalDateTime dataUltimaAtualizacaoPerfil,
        /* Ids dos leads criados automaticamente nesta atualização (vazio em consultas). */
        List<Long> leadsGerados
) {
}
