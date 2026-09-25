package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.dto.PageResponse;
import br.com.fiap.fordretention.dto.concessionaria.ConcessionariaFiltro;
import br.com.fiap.fordretention.dto.concessionaria.ConcessionariaPatchRequest;
import br.com.fiap.fordretention.dto.concessionaria.ConcessionariaRequest;
import br.com.fiap.fordretention.dto.concessionaria.ConcessionariaResponse;
import br.com.fiap.fordretention.dto.concessionaria.ServiceShareResponse;
import br.com.fiap.fordretention.service.ConcessionariaService;
import br.com.fiap.fordretention.service.ServiceShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/concessionarias")
@Tag(name = "Concessionárias", description = "Rede de concessionárias Ford e indicador de Service Share")
@ApiResponsesPadrao
public class ConcessionariaController {

    private final ConcessionariaService concessionariaService;
    private final ServiceShareService serviceShareService;

    public ConcessionariaController(ConcessionariaService concessionariaService,
                                    ServiceShareService serviceShareService) {
        this.concessionariaService = concessionariaService;
        this.serviceShareService = serviceShareService;
    }

    @GetMapping
    @Operation(summary = "Lista concessionárias (gestor/consultor veem apenas a própria)")
    public ResponseEntity<PageResponse<ConcessionariaResponse>> listar(
            @ParameterObject ConcessionariaFiltro filtro,
            @ParameterObject @PageableDefault(sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(concessionariaService.listar(filtro, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca concessionária por id")
    @ApiResponse(responseCode = "404", description = "Não encontrada ou fora do seu escopo")
    public ResponseEntity<ConcessionariaResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(concessionariaService.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra concessionária (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Criada; header Location com a URI do recurso")
    @ApiResponse(responseCode = "409", description = "CNPJ já cadastrado")
    public ResponseEntity<ConcessionariaResponse> criar(@Valid @RequestBody ConcessionariaRequest request) {
        ConcessionariaResponse criada = concessionariaService.criar(request);
        return ResponseEntity.created(Localizacao.doNovoRecurso(criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Substitui todos os dados da concessionária (ADMIN)")
    public ResponseEntity<ConcessionariaResponse> atualizar(@PathVariable Long id,
                                                            @Valid @RequestBody ConcessionariaRequest request) {
        return ResponseEntity.ok(concessionariaService.atualizar(id, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza parcialmente nome, cidade e/ou estado (ADMIN)")
    public ResponseEntity<ConcessionariaResponse> atualizarParcial(@PathVariable Long id,
                                                                   @Valid @RequestBody ConcessionariaPatchRequest request) {
        return ResponseEntity.ok(concessionariaService.atualizarParcial(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove concessionária sem vínculos (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Removida")
    @ApiResponse(responseCode = "409", description = "Possui clientes, serviços, leads ou usuários vinculados")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        concessionariaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/service-share")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Calcula o Service Share da base de clientes da concessionária",
            description = "% de clientes da base com ao menos um serviço PAGO na rede Ford no período, "
                    + "com recortes por idade do veículo (0-3, 4-6, 7+ anos), modelo e tipo de serviço.")
    public ResponseEntity<ServiceShareResponse> serviceShare(
            @PathVariable Long id,
            @Parameter(description = "Janela em meses (1 a 60)", example = "12")
            @RequestParam(defaultValue = "12") @Min(1) @Max(60) int meses) {
        return ResponseEntity.ok(serviceShareService.calcular(id, meses));
    }
}
