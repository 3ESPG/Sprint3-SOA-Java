package br.com.fiap.fordretention.mapper;

import br.com.fiap.fordretention.dto.veiculo.VeiculoResponse;
import br.com.fiap.fordretention.model.Veiculo;
import org.springframework.stereotype.Component;

@Component
public class VeiculoMapper {

    public VeiculoResponse toResponse(Veiculo v, int anoAtual) {
        return new VeiculoResponse(
                v.getId(),
                v.getVin(),
                v.getModelo(),
                v.getAno(),
                v.idade(anoAtual),
                v.getQuilometragem(),
                v.getStatusGarantia(),
                v.getCliente().getId(),
                v.getCliente().getNome());
    }
}
