package br.com.fiap.fordretention.dto.veiculo;

import br.com.fiap.fordretention.model.enums.StatusGarantia;

public record VeiculoResponse(
        Long id,
        String vin,
        String modelo,
        Integer ano,
        int idade,
        Integer quilometragem,
        StatusGarantia statusGarantia,
        Long clienteId,
        String clienteNome
) {
}
