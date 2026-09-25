package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.dto.PageResponse;
import br.com.fiap.fordretention.dto.usuario.UsuarioAtivacaoRequest;
import br.com.fiap.fordretention.dto.usuario.UsuarioRequest;
import br.com.fiap.fordretention.dto.usuario.UsuarioResponse;
import br.com.fiap.fordretention.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuários", description = "Gestão de acessos (ADMIN e gestores)")
@ApiResponsesPadrao
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Lista usuários (gestor vê apenas os da própria concessionária)")
    public ResponseEntity<PageResponse<UsuarioResponse>> listar(
            @ParameterObject @PageableDefault(sort = "id") Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listar(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca usuário por id (consultor só consegue ver a si mesmo)")
    public ResponseEntity<UsuarioResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria usuário com qualquer perfil (somente ADMIN)")
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse usuario = usuarioService.criar(request);
        return ResponseEntity.created(Localizacao.doNovoRecurso(usuario.id())).body(usuario);
    }

    @PatchMapping("/{id}/ativacao")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Aprova (ativa) ou bloqueia um usuário",
            description = "Gestores só podem ativar/desativar consultores da própria concessionária.")
    public ResponseEntity<UsuarioResponse> alterarAtivacao(@PathVariable Long id,
                                                           @Valid @RequestBody UsuarioAtivacaoRequest request) {
        return ResponseEntity.ok(usuarioService.alterarAtivacao(id, request.ativo()));
    }
}
