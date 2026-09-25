package br.com.fiap.fordretention.mapper;

import br.com.fiap.fordretention.dto.lead.LeadResponse;
import br.com.fiap.fordretention.model.Cliente;
import br.com.fiap.fordretention.model.Lead;
import br.com.fiap.fordretention.model.Veiculo;
import org.springframework.stereotype.Component;

@Component
public class LeadMapper {

    public LeadResponse toResponse(Lead lead) {
        Cliente cliente = lead.getCliente();
        Veiculo veiculo = lead.getVeiculo();
        return new LeadResponse(
                lead.getId(),
                cliente.getId(),
                cliente.getNome(),
                cliente.getTelefone(),
                cliente.getPerfil(),
                cliente.getScoreRisco(),
                veiculo.getId(),
                veiculo.getVin(),
                veiculo.getModelo(),
                veiculo.getAno(),
                lead.getConcessionaria().getId(),
                lead.getConcessionaria().getNome(),
                lead.getMotivo(),
                lead.getPrioridade(),
                lead.getStatus(),
                lead.getOrigem(),
                lead.getObservacao(),
                lead.getDataCriacao(),
                lead.getDataAtualizacao());
    }
}
