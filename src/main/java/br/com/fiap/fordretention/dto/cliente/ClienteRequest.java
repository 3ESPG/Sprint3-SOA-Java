package br.com.fiap.fordretention.dto.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @Schema(example = "João Pereira")
        @NotBlank @Size(max = 120)
        String nome,

        @Schema(example = "joao.pereira@email.com")
        @NotBlank @Email @Size(max = 150)
        String email,

        @Schema(example = "+5511988887777")
        @Pattern(regexp = "^\\+?[0-9 ()-]{10,20}$", message = "telefone inválido")
        String telefone,

        @Schema(example = "1")
        @NotNull
        Long concessionariaPreferidaId
) {
}
