package br.com.fiap.fordretention.dto.concessionaria;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConcessionariaFiltro(
        @Schema(description = "Parte do nome (sem diferenciar maiúsculas)")
        String nome,
        String cidade,
        @Schema(example = "SP")
        String estado
) {
}
