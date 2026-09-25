package br.com.fiap.fordretention.mapper;

import br.com.fiap.fordretention.dto.cliente.ClienteResponse;
import br.com.fiap.fordretention.dto.cliente.PerfilResponse;
import br.com.fiap.fordretention.model.Cliente;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClienteMapper {

    public ClienteResponse toResponse(Cliente c) {
        return new ClienteResponse(
                c.getId(),
                c.getNome(),
                c.getEmail(),
                c.getTelefone(),
                c.getConcessionariaPreferida().getId(),
                c.getConcessionariaPreferida().getNome(),
                c.getPerfil(),
                c.getScoreRisco(),
                c.getDataUltimaAtualizacaoPerfil());
    }

    public PerfilResponse toPerfilResponse(Cliente c, List<Long> leadsGerados) {
        return new PerfilResponse(
                c.getId(),
                c.getNome(),
                c.getPerfil(),
                c.getPerfil().getNome(),
                c.getPerfil().getDescricao(),
                c.getScoreRisco(),
                c.getDataUltimaAtualizacaoPerfil(),
                leadsGerados);
    }
}
