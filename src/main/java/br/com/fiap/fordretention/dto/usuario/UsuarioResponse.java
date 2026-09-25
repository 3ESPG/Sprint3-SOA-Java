package br.com.fiap.fordretention.dto.usuario;

import br.com.fiap.fordretention.model.enums.Role;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Role role,
        Long concessionariaId,
        boolean ativo
) {
}
