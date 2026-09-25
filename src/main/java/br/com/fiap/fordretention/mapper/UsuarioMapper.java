package br.com.fiap.fordretention.mapper;

import br.com.fiap.fordretention.dto.usuario.UsuarioResponse;
import br.com.fiap.fordretention.model.Usuario;
import br.com.fiap.fordretention.security.UsuarioAutenticado;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getRole(), u.getConcessionariaId(),
                u.isAtivo());
    }

    public UsuarioResponse toResponse(UsuarioAutenticado u) {
        return new UsuarioResponse(u.id(), u.nome(), u.email(), u.role(), u.concessionariaId(), u.ativo());
    }
}
