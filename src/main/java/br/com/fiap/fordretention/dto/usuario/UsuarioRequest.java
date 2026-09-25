package br.com.fiap.fordretention.dto.usuario;

import br.com.fiap.fordretention.model.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Criação de usuário por um ADMIN (qualquer role). GESTOR e CONSULTOR exigem concessionariaId. */
public record UsuarioRequest(
        @NotBlank @Size(max = 120)
        String nome,

        @NotBlank @Email @Size(max = 150)
        String email,

        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "deve conter letras e números")
        String senha,

        @Schema(example = "GESTOR_CONCESSIONARIA")
        @NotNull
        Role role,

        Long concessionariaId
) {
}
