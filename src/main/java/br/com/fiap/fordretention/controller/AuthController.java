package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.dto.auth.LoginRequest;
import br.com.fiap.fordretention.dto.auth.RegisterRequest;
import br.com.fiap.fordretention.dto.auth.TokenResponse;
import br.com.fiap.fordretention.dto.usuario.UsuarioResponse;
import br.com.fiap.fordretention.exception.ApiError;
import br.com.fiap.fordretention.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoints públicos de login e auto-cadastro")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e retorna um JWT",
            description = "Token com claims sub, uid, role, concessionariaId, iat e exp. Use-o como Bearer.")
    @ApiResponse(responseCode = "200", description = "Autenticado")
    @ApiResponse(responseCode = "400", description = "Payload inválido",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou usuário pendente de aprovação",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Auto-cadastro de consultor",
            description = "Cria um CONSULTOR inativo vinculado à concessionária informada. "
                    + "O acesso é liberado após aprovação (PATCH /usuarios/{id}/ativacao) por um ADMIN ou pelo gestor.")
    @ApiResponse(responseCode = "201", description = "Cadastro criado (header Location aponta para /usuarios/{id})")
    @ApiResponse(responseCode = "400", description = "Payload inválido",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "Concessionária inexistente",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegisterRequest request) {
        UsuarioResponse usuario = authService.registrar(request);
        return ResponseEntity.created(Localizacao.de("/usuarios", usuario.id())).body(usuario);
    }
}
