package br.com.fiap.fordretention.dto.concessionaria;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;

public record ConcessionariaRequest(
        @Schema(example = "Ford Central Paulista")
        @NotBlank @Size(max = 120)
        String nome,

        @Schema(example = "São Paulo")
        @NotBlank @Size(max = 80)
        String cidade,

        @Schema(example = "SP")
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "deve ser a sigla da UF com 2 letras")
        String estado,

        @Schema(example = "11.222.333/0001-81", description = "Com ou sem pontuação; dígitos verificadores são validados")
        @NotBlank @CNPJ
        String cnpj
) {
}
