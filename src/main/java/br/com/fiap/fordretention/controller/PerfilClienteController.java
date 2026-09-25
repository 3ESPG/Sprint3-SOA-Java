package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.dto.cliente.PerfilRequest;
import br.com.fiap.fordretention.dto.cliente.PerfilResponse;
import br.com.fiap.fordretention.service.PerfilClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes/{clienteId}/perfil")
@Tag(name = "Perfil do cliente (ML)", description = "Integração com o modelo Random Forest de retenção")
@ApiResponsesPadrao
public class PerfilClienteController {

    private final PerfilClienteService perfilClienteService;

    public PerfilClienteController(PerfilClienteService perfilClienteService) {
        this.perfilClienteService = perfilClienteService;
    }

    @GetMapping
    @Operation(summary = "Consulta o perfil de retenção atual do cliente")
    @ApiResponse(responseCode = "404", description = "Cliente inexistente/fora do escopo ou perfil ainda não calculado")
    public ResponseEntity<PerfilResponse> buscar(@PathVariable Long clienteId) {
        return ResponseEntity.ok(perfilClienteService.buscar(clienteId));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registra o perfil/score calculado pelo modelo de ML (ADMIN / serviço de ML)",
            description = "Se o perfil for ABANDONO ou ESQUECIDO e o score ≥ app.lead.score-limite (0.70), "
                    + "gera automaticamente um lead para cada veículo do cliente sem lead ativo. "
                    + "Os ids criados voltam em leadsGerados.")
    public ResponseEntity<PerfilResponse> atualizar(@PathVariable Long clienteId,
                                                    @Valid @RequestBody PerfilRequest request) {
        return ResponseEntity.ok(perfilClienteService.atualizar(clienteId, request));
    }
}
