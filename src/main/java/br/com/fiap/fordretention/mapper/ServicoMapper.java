package br.com.fiap.fordretention.mapper;

import br.com.fiap.fordretention.dto.servico.ServicoResponse;
import br.com.fiap.fordretention.model.Servico;
import org.springframework.stereotype.Component;

@Component
public class ServicoMapper {

    public ServicoResponse toResponse(Servico s) {
        return new ServicoResponse(
                s.getId(),
                s.getVeiculo().getId(),
                s.getVeiculo().getVin(),
                s.getVeiculo().getModelo(),
                s.getConcessionaria().getId(),
                s.getConcessionaria().getNome(),
                s.getTipo(),
                s.getValor(),
                s.getData(),
                s.isPago(),
                s.getDescricao());
    }
}
