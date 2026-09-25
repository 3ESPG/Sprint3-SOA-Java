package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.dto.PageResponse;
import br.com.fiap.fordretention.dto.cliente.ClienteFiltro;
import br.com.fiap.fordretention.dto.cliente.ClientePatchRequest;
import br.com.fiap.fordretention.dto.cliente.ClienteRequest;
import br.com.fiap.fordretention.dto.cliente.ClienteResponse;
import br.com.fiap.fordretention.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Base de clientes com perfil de retenção")
@ApiResponsesPadrao
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    @Operation(summary = "Lista clientes com filtros (ex.: ?perfil=ABANDONO&scoreMin=0.7)")
    public ResponseEntity<PageResponse<ClienteResponse>> listar(
            @ParameterObject @Valid ClienteFiltro filtro,
            @ParameterObject @PageableDefault(sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(clienteService.listar(filtro, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca cliente por id")
    @ApiResponse(responseCode = "404", description = "Não encontrado ou fora do seu escopo")
    public ResponseEntity<ClienteResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Cadastra cliente")
    @ApiResponse(responseCode = "201", description = "Criado; header Location com a URI do recurso")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    @ApiResponse(responseCode = "422", description = "Concessionária inexistente")
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse criado = clienteService.criar(request);
        return ResponseEntity.created(Localizacao.doNovoRecurso(criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Substitui os dados cadastrais do cliente")
    public ResponseEntity<ClienteResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(id, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Atualiza parcialmente os dados cadastrais do cliente")
    public ResponseEntity<ClienteResponse> atualizarParcial(@PathVariable Long id,
                                                            @Valid @RequestBody ClientePatchRequest request) {
        return ResponseEntity.ok(clienteService.atualizarParcial(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Remove cliente sem veículos/leads")
    @ApiResponse(responseCode = "204", description = "Removido")
    @ApiResponse(responseCode = "409", description = "Possui veículos ou leads vinculados")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
