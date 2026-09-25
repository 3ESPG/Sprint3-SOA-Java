package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.PageResponse;
import br.com.fiap.fordretention.dto.veiculo.VeiculoFiltro;
import br.com.fiap.fordretention.dto.veiculo.VeiculoPatchRequest;
import br.com.fiap.fordretention.dto.veiculo.VeiculoRequest;
import br.com.fiap.fordretention.dto.veiculo.VeiculoResponse;
import br.com.fiap.fordretention.exception.ConflitoException;
import br.com.fiap.fordretention.exception.RecursoNaoEncontradoException;
import br.com.fiap.fordretention.exception.RegraNegocioException;
import br.com.fiap.fordretention.mapper.VeiculoMapper;
import br.com.fiap.fordretention.model.Cliente;
import br.com.fiap.fordretention.model.Veiculo;
import br.com.fiap.fordretention.repository.LeadRepository;
import br.com.fiap.fordretention.repository.ServicoRepository;
import br.com.fiap.fordretention.repository.VeiculoRepository;
import br.com.fiap.fordretention.repository.spec.VeiculoSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Year;
import java.util.Locale;

@Service
public class VeiculoService {

    private static final String RECURSO = "Veículo";

    private final VeiculoRepository veiculoRepository;
    private final ServicoRepository servicoRepository;
    private final LeadRepository leadRepository;
    private final ClienteService clienteService;
    private final VeiculoMapper mapper;
    private final EscopoAcessoService escopo;
    private final Clock clock;

    public VeiculoService(VeiculoRepository veiculoRepository, ServicoRepository servicoRepository,
                          LeadRepository leadRepository, ClienteService clienteService, VeiculoMapper mapper,
                          EscopoAcessoService escopo, Clock clock) {
        this.veiculoRepository = veiculoRepository;
        this.servicoRepository = servicoRepository;
        this.leadRepository = leadRepository;
        this.clienteService = clienteService;
        this.mapper = mapper;
        this.escopo = escopo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<VeiculoResponse> listar(VeiculoFiltro filtro, Pageable pageable) {
        if (filtro != null && filtro.idadeMin() != null && filtro.idadeMax() != null
                && filtro.idadeMin() > filtro.idadeMax()) {
            throw new RegraNegocioException("idadeMin não pode ser maior que idadeMax");
        }
        int anoAtual = anoAtual();
        Long concessionariaId = escopo.resolverFiltro(filtro == null ? null : filtro.concessionariaId());
        var spec = VeiculoSpecifications.filtrar(filtro, concessionariaId, anoAtual);
        return PageResponse.of(veiculoRepository.findAll(spec, pageable).map(v -> mapper.toResponse(v, anoAtual)));
    }

    @Transactional(readOnly = true)
    public VeiculoResponse buscar(Long id) {
        return mapper.toResponse(buscarNoEscopo(id), anoAtual());
    }

    @Transactional
    public VeiculoResponse criar(VeiculoRequest request) {
        Cliente cliente = clienteService.buscarReferencia(request.clienteId());
        validarAno(request.ano());
        String vin = normalizarVin(request.vin());
        if (veiculoRepository.existsByVin(vin)) {
            throw new ConflitoException("Já existe um veículo com o VIN " + vin);
        }
        Veiculo veiculo = new Veiculo(vin, request.modelo().trim(), request.ano(), request.quilometragem(), cliente,
                request.statusGarantia());
        return mapper.toResponse(veiculoRepository.save(veiculo), anoAtual());
    }

    @Transactional
    public VeiculoResponse atualizar(Long id, VeiculoRequest request) {
        Veiculo veiculo = buscarNoEscopo(id);
        Cliente cliente = clienteService.buscarReferencia(request.clienteId());
        validarAno(request.ano());
        String vin = normalizarVin(request.vin());
        if (veiculoRepository.existsByVinAndIdNot(vin, id)) {
            throw new ConflitoException("Já existe outro veículo com o VIN " + vin);
        }
        validarQuilometragem(veiculo, request.quilometragem());
        veiculo.setVin(vin);
        veiculo.setModelo(request.modelo().trim());
        veiculo.setAno(request.ano());
        veiculo.setQuilometragem(request.quilometragem());
        veiculo.setCliente(cliente);
        veiculo.setStatusGarantia(request.statusGarantia());
        return mapper.toResponse(veiculo, anoAtual());
    }

    @Transactional
    public VeiculoResponse atualizarParcial(Long id, VeiculoPatchRequest request) {
        Veiculo veiculo = buscarNoEscopo(id);
        if (request.quilometragem() != null) {
            validarQuilometragem(veiculo, request.quilometragem());
            veiculo.setQuilometragem(request.quilometragem());
        }
        if (request.statusGarantia() != null) {
            veiculo.setStatusGarantia(request.statusGarantia());
        }
        return mapper.toResponse(veiculo, anoAtual());
    }

    @Transactional
    public void excluir(Long id) {
        Veiculo veiculo = buscarNoEscopo(id);
        if (servicoRepository.existsByVeiculoId(id) || leadRepository.existsByVeiculoId(id)) {
            throw new ConflitoException("Veículo possui histórico de serviços ou leads e não pode ser excluído");
        }
        veiculoRepository.delete(veiculo);
    }

    /** Leitura/alteração do próprio recurso: escopo pela concessionária preferida do dono → 404 fora dele. */
    @Transactional(readOnly = true)
    public Veiculo buscarNoEscopo(Long id) {
        Veiculo veiculo = veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(RECURSO, id));
        if (!escopo.podeAcessar(veiculo.getCliente().getConcessionariaPreferidaId())) {
            throw new RecursoNaoEncontradoException(RECURSO, id);
        }
        return veiculo;
    }

    /**
     * Veículo referenciado no corpo de outra requisição (ex.: registro de serviço). Qualquer concessionária
     * da rede pode atender qualquer veículo Ford, por isso não há restrição de escopo aqui. Inexistente → 422.
     */
    @Transactional(readOnly = true)
    public Veiculo buscarReferencia(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Veículo %d não existe".formatted(id)));
    }

    private void validarAno(int ano) {
        int limite = anoAtual() + 1;
        if (ano > limite) {
            throw new RegraNegocioException("Ano do veículo não pode ser posterior a " + limite);
        }
    }

    private static void validarQuilometragem(Veiculo veiculo, int novaQuilometragem) {
        if (novaQuilometragem < veiculo.getQuilometragem()) {
            throw new RegraNegocioException("Quilometragem não pode diminuir (atual: %d km)"
                    .formatted(veiculo.getQuilometragem()));
        }
    }

    private int anoAtual() {
        return Year.now(clock).getValue();
    }

    private static String normalizarVin(String vin) {
        return vin.trim().toUpperCase(Locale.ROOT);
    }
}
