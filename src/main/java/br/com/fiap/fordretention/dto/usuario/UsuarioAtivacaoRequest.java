package br.com.fiap.fordretention.dto.usuario;

import jakarta.validation.constraints.NotNull;

public record UsuarioAtivacaoRequest(@NotNull Boolean ativo) {
}
