package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.exception.ApiError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Respostas de erro comuns a todos os endpoints protegidos, documentadas no Swagger com o schema ApiError. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Perfil sem permissão para a operação",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
})
public @interface ApiResponsesPadrao {
}
