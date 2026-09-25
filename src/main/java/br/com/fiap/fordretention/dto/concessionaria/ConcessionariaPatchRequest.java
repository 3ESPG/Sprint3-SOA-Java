package br.com.fiap.fordretention.dto.concessionaria;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Atualização parcial: apenas os campos informados (não nulos) são alterados. */
public record ConcessionariaPatchRequest(
        @Size(min = 1, max = 120)
        String nome,

        @Size(min = 1, max = 80)
        String cidade,

        @Pattern(regexp = "^[A-Za-z]{2}$", message = "deve ser a sigla da UF com 2 letras")
        String estado
) {
}
