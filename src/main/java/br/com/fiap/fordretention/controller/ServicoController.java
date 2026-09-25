package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.dto.PageResponse;
import br.com.fiap.fordretention.dto.servico.ServicoFiltro;
import br.com.fiap.fordretention.dto.servico.ServicoRequest;
import br.com.fiap.fordretention.dto.servico.ServicoResponse;
import br.com.fiap.fordretention.service.ServicoService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/servicos")
@Tag(name = "Serviços", description = "Histórico de serviços executados na rede Ford")
@ApiResponsesPadrao
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @GetMapping
    @Operation(summary = "Lista serviços com filtros (ex.: ?tipo=REVISAO&pago=true&dataInicio=2025-10-01)")
    public ResponseEntity<PageResponse<ServicoResponse>> listar(
            @ParameterObject @Valid ServicoFiltro filtro,
            @ParameterObject @PageableDefault(sort = "data", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(servicoService.listar(filtro, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca serviço por id")
    @ApiResponse(responseCode = "404", description = "Não encontrado ou fora do seu escopo")
    public ResponseEntity<ServicoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(servicoService.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Registra serviço",
            description = "Se o serviço for pago, os leads ativos do veículo são marcados como CONVERTIDO.")
    @ApiResponse(responseCode = "201", description = "Criado; header Location com a URI do recurso")
    @ApiResponse(responseCode = "422", description = "Data futura, RECALL/GARANTIA pagos ou garantia expirada")
    public ResponseEntity<ServicoResponse> criar(@Valid @RequestBody ServicoRequest request) {
        ServicoResponse criado = servicoService.criar(request);
        return ResponseEntity.created(Localizacao.doNovoRecurso(criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Substitui os dados do serviço")
    public ResponseEntity<ServicoResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.ok(servicoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Remove serviço")
    @ApiResponse(responseCode = "204", description = "Removido")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servicoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
