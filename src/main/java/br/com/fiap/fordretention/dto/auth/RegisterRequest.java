package br.com.fiap.fordretention.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Auto-cadastro público: sempre cria um CONSULTOR inativo, que precisa ser aprovado. */
public record RegisterRequest(
        @Schema(example = "Marina Costa")
        @NotBlank @Size(max = 120)
        String nome,

        @Schema(example = "marina.costa@fordcentral.com.br")
        @NotBlank @Email @Size(max = 150)
        String email,

        @Schema(example = "Consultor@2026", description = "Mínimo 8 caracteres, com letras e números")
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "deve conter letras e números")
        String senha,

        @Schema(example = "1")
        @NotNull
        Long concessionariaId
) {
}
