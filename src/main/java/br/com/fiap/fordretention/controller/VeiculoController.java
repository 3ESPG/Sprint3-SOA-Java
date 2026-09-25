package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.dto.PageResponse;
import br.com.fiap.fordretention.dto.veiculo.VeiculoFiltro;
import br.com.fiap.fordretention.dto.veiculo.VeiculoPatchRequest;
import br.com.fiap.fordretention.dto.veiculo.VeiculoRequest;
import br.com.fiap.fordretention.dto.veiculo.VeiculoResponse;
import br.com.fiap.fordretention.service.VeiculoService;
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
@RequestMapping("/veiculos")
@Tag(name = "Veículos", description = "Parque de veículos (VIN) dos clientes")
@ApiResponsesPadrao
public class VeiculoController {

    private final VeiculoService veiculoService;

    public VeiculoController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @GetMapping
    @Operation(summary = "Lista veículos com filtros (ex.: ?modelo=Ranger&idadeMin=4)")
    public ResponseEntity<PageResponse<VeiculoResponse>> listar(
            @ParameterObject @Valid VeiculoFiltro filtro,
            @ParameterObject @PageableDefault(sort = "id") Pageable pageable) {
        return ResponseEntity.ok(veiculoService.listar(filtro, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca veículo por id")
    @ApiResponse(responseCode = "404", description = "Não encontrado ou fora do seu escopo")
    public ResponseEntity<VeiculoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(veiculoService.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Cadastra veículo")
    @ApiResponse(responseCode = "201", description = "Criado; header Location com a URI do recurso")
    @ApiResponse(responseCode = "409", description = "VIN já cadastrado")
    @ApiResponse(responseCode = "422", description = "Cliente inexistente ou ano inválido")
    public ResponseEntity<VeiculoResponse> criar(@Valid @RequestBody VeiculoRequest request) {
        VeiculoResponse criado = veiculoService.criar(request);
        return ResponseEntity.created(Localizacao.doNovoRecurso(criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Substitui os dados do veículo")
    public ResponseEntity<VeiculoResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody VeiculoRequest request) {
        return ResponseEntity.ok(veiculoService.atualizar(id, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Atualiza quilometragem e/ou status da garantia")
    @ApiResponse(responseCode = "422", description = "Quilometragem menor que a atual")
    public ResponseEntity<VeiculoResponse> atualizarParcial(@PathVariable Long id,
                                                            @Valid @RequestBody VeiculoPatchRequest request) {
        return ResponseEntity.ok(veiculoService.atualizarParcial(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_CONCESSIONARIA')")
    @Operation(summary = "Remove veículo sem histórico")
    @ApiResponse(responseCode = "204", description = "Removido")
    @ApiResponse(responseCode = "409", description = "Possui serviços ou leads")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        veiculoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
