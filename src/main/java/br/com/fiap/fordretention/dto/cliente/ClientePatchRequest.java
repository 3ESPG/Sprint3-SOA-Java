package br.com.fiap.fordretention.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Atualização parcial: apenas os campos informados (não nulos) são alterados. */
public record ClientePatchRequest(
        @Size(min = 1, max = 120)
        String nome,

        @Email @Size(max = 150)
        String email,

        @Pattern(regexp = "^\\+?[0-9 ()-]{10,20}$", message = "telefone inválido")
        String telefone,

        Long concessionariaPreferidaId
) {
}
