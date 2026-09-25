package br.com.fiap.fordretention.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/** Formato único de erro da API (inclusive para 401/403 gerados pela camada de segurança). */
@Schema(name = "ApiError", description = "Formato padrão de erro")
public record ApiError(
        @Schema(example = "2026-09-24T21:40:00Z")
        Instant timestamp,
        @Schema(example = "400")
        int status,
        @Schema(example = "Bad Request")
        String error,
        @Schema(example = "Dados inválidos")
        String message,
        @Schema(example = "/clientes")
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<CampoErro> fieldErrors
) {
    public record CampoErro(
            @Schema(example = "email") String field,
            @Schema(example = "deve ser um endereço de e-mail bem formado") String message) {
    }

    public static ApiError of(HttpStatus status, String message, String path) {
        return of(status, message, path, List.of());
    }

    public static ApiError of(HttpStatus status, String message, String path, List<CampoErro> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, fieldErrors);
    }
}
