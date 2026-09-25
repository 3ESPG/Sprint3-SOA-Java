package br.com.fiap.fordretention.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "admin@ford.com")
        @NotBlank @Email @Size(max = 150)
        String email,

        @Schema(example = "Ford@2026")
        @NotBlank @Size(max = 72, message = "senha deve ter no máximo 72 caracteres")
        String senha
) {
}
