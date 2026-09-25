package br.com.fiap.fordretention.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "admin@ford.com")
        @NotBlank @Email
        String email,

        @Schema(example = "Ford@2026")
        @NotBlank
        String senha
) {
}
