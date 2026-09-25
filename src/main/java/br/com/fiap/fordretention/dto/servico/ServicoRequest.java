package br.com.fiap.fordretention.dto.servico;

import br.com.fiap.fordretention.model.enums.TipoServico;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServicoRequest(
        @Schema(example = "1")
        @NotNull
        Long veiculoId,

        @Schema(example = "1", description = "Concessionária que executou o serviço")
        @NotNull
        Long concessionariaId,

        @Schema(example = "REVISAO")
        @NotNull
        TipoServico tipo,

        @Schema(example = "890.00")
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2)
        BigDecimal valor,

        @Schema(example = "2026-09-10", description = "Não pode ser futura")
        @NotNull
        LocalDate data,

        @Schema(example = "true")
        @NotNull
        Boolean pago,

        @Size(max = 255)
        String descricao
) {
}
