package br.com.fiap.fordretention.dto.cliente;

import br.com.fiap.fordretention.model.enums.PerfilCliente;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        Long concessionariaPreferidaId,
        String concessionariaPreferidaNome,
        PerfilCliente perfil,
        BigDecimal scoreRisco,
        LocalDateTime dataUltimaAtualizacaoPerfil
) {
}
