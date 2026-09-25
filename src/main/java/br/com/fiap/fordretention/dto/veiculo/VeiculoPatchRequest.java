package br.com.fiap.fordretention.dto.veiculo;

import br.com.fiap.fordretention.model.enums.StatusGarantia;
import jakarta.validation.constraints.PositiveOrZero;

/** Atualizações parciais mais comuns no pós-venda: hodômetro e garantia. */
public record VeiculoPatchRequest(
        @PositiveOrZero
        Integer quilometragem,

        StatusGarantia statusGarantia
) {
}
