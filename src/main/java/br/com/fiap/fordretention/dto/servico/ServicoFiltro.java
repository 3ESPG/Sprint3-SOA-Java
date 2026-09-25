package br.com.fiap.fordretention.dto.servico;

import br.com.fiap.fordretention.model.enums.TipoServico;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ServicoFiltro(
        @Schema(example = "REVISAO")
        TipoServico tipo,

        Boolean pago,

        @Schema(example = "2025-10-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dataInicio,

        @Schema(example = "2026-09-30")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dataFim,

        Long veiculoId,

        @Schema(description = "Apenas ADMIN; para os demais perfis é sempre a concessionária do token")
        Long concessionariaId
) {
}
