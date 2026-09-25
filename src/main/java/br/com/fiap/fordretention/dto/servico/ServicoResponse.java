package br.com.fiap.fordretention.dto.servico;

import br.com.fiap.fordretention.model.enums.TipoServico;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServicoResponse(
        Long id,
        Long veiculoId,
        String vin,
        String modelo,
        Long concessionariaId,
        String concessionariaNome,
        TipoServico tipo,
        BigDecimal valor,
        LocalDate data,
        boolean pago,
        String descricao
) {
}
