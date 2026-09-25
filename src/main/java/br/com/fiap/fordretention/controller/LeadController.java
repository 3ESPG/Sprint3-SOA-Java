package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.dto.PageResponse;
import br.com.fiap.fordretention.dto.lead.LeadFiltro;
import br.com.fiap.fordretention.dto.lead.LeadRequest;
import br.com.fiap.fordretention.dto.lead.LeadResponse;
import br.com.fiap.fordretention.dto.lead.LeadStatusRequest;
import br.com.fiap.fordretention.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/leads")
@Tag(name = "Leads", description = "Leads proativos de serviço para as concessionárias")
@ApiResponsesPadrao
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    @Operation(summary = "Lista leads com filtros (ex.: ?status=ABERTO&perfil=ABANDONO&prioridade=ALTA)")
    public ResponseEntity<PageResponse<LeadResponse>> listar(
            @ParameterObject LeadFiltro filtro,
            @ParameterObject @PageableDefault(sort = "dataCriacao", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(leadService.listar(filtro, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca lead por id")
    @ApiResponse(responseCode = "404", description = "Não encontrado ou fora do seu escopo")
    public ResponseEntity<LeadResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Cria lead manual")
    @ApiResponse(responseCode = "201", description = "Criado; header Location com a URI do recurso")
    @ApiResponse(responseCode = "409", description = "Já existe lead ativo para o veículo")
    @ApiResponse(responseCode = "422", description = "Cliente/veículo inexistente ou veículo não pertence ao cliente")
    public ResponseEntity<LeadResponse> criar(@Valid @RequestBody LeadRequest request) {
        LeadResponse criado = leadService.criar(request);
        return ResponseEntity.created(Localizacao.doNovoRecurso(criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Substitui cliente, veículo, motivo e prioridade de um lead ativo")
    public ResponseEntity<LeadResponse> atualizar(@PathVariable Long id, @Valid @RequestBody LeadRequest request) {
        return ResponseEntity.ok(leadService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA', 'CONSULTOR')")
    @Operation(summary = "Avança o lead no funil (uso principal do consultor no app)",
            description = "ABERTO → CONTATADO | AGENDADO | PERDIDO; CONTATADO → AGENDADO | PERDIDO; "
                    + "AGENDADO → CONVERTIDO | CONTATADO | PERDIDO.")
    @ApiResponse(responseCode = "422", description = "Transição de status não permitida")
    public ResponseEntity<LeadResponse> alterarStatus(@PathVariable Long id,
                                                      @Valid @RequestBody LeadStatusRequest request) {
        return ResponseEntity.ok(leadService.alterarStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove lead (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Removido")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        leadService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
