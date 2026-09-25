package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.PageResponse;
import br.com.fiap.fordretention.dto.servico.ServicoFiltro;
import br.com.fiap.fordretention.dto.servico.ServicoRequest;
import br.com.fiap.fordretention.dto.servico.ServicoResponse;
import br.com.fiap.fordretention.exception.RecursoNaoEncontradoException;
import br.com.fiap.fordretention.exception.RegraNegocioException;
import br.com.fiap.fordretention.mapper.ServicoMapper;
import br.com.fiap.fordretention.model.Concessionaria;
import br.com.fiap.fordretention.model.Servico;
import br.com.fiap.fordretention.model.Veiculo;
import br.com.fiap.fordretention.model.enums.TipoServico;
import br.com.fiap.fordretention.repository.ConcessionariaRepository;
import br.com.fiap.fordretention.repository.ServicoRepository;
import br.com.fiap.fordretention.repository.spec.ServicoSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class ServicoService {

    private static final String RECURSO = "Serviço";

    private final ServicoRepository servicoRepository;
    private final ConcessionariaRepository concessionariaRepository;
    private final VeiculoService veiculoService;
    private final LeadService leadService;
    private final ServicoMapper mapper;
    private final EscopoAcessoService escopo;
    private final Clock clock;

    public ServicoService(ServicoRepository servicoRepository, ConcessionariaRepository concessionariaRepository,
                          VeiculoService veiculoService, LeadService leadService, ServicoMapper mapper,
                          EscopoAcessoService escopo, Clock clock) {
        this.servicoRepository = servicoRepository;
        this.concessionariaRepository = concessionariaRepository;
        this.veiculoService = veiculoService;
        this.leadService = leadService;
        this.mapper = mapper;
        this.escopo = escopo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ServicoResponse> listar(ServicoFiltro filtro, Pageable pageable) {
        if (filtro != null && filtro.dataInicio() != null && filtro.dataFim() != null
                && filtro.dataInicio().isAfter(filtro.dataFim())) {
            throw new RegraNegocioException("dataInicio não pode ser posterior a dataFim");
        }
        Long concessionariaId = escopo.resolverFiltro(filtro == null ? null : filtro.concessionariaId());
        var spec = ServicoSpecifications.filtrar(filtro, concessionariaId);
        return PageResponse.of(servicoRepository.findAll(spec, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscar(Long id) {
        return mapper.toResponse(buscarNoEscopo(id));
    }

    /** Registra o serviço; se for pago, converte os leads ativos do veículo (fecha o ciclo do lead). */
    @Transactional
    public ServicoResponse criar(ServicoRequest request) {
        Concessionaria concessionaria = referenciarConcessionaria(request.concessionariaId());
        Veiculo veiculo = veiculoService.buscarReferencia(request.veiculoId());
        validar(request, veiculo);
        Servico servico = servicoRepository.save(new Servico(veiculo, concessionaria, request.tipo(),
                request.valor(), request.data(), request.pago(), request.descricao()));
        if (servico.isPago()) {
            leadService.converterLeadsAtivos(veiculo.getId(), servico.getId());
        }
        return mapper.toResponse(servico);
    }

    @Transactional
    public ServicoResponse atualizar(Long id, ServicoRequest request) {
        Servico servico = buscarNoEscopo(id);
        Concessionaria concessionaria = referenciarConcessionaria(request.concessionariaId());
        Veiculo veiculo = veiculoService.buscarReferencia(request.veiculoId());
        validar(request, veiculo);
        boolean passouAPago = !servico.isPago() && request.pago();
        servico.setVeiculo(veiculo);
        servico.setConcessionaria(concessionaria);
        servico.setTipo(request.tipo());
        servico.setValor(request.valor());
        servico.setData(request.data());
        servico.setPago(request.pago());
        servico.setDescricao(request.descricao());
        if (passouAPago) {
            leadService.converterLeadsAtivos(veiculo.getId(), servico.getId());
        }
        return mapper.toResponse(servico);
    }

    @Transactional
    public void excluir(Long id) {
        servicoRepository.delete(buscarNoEscopo(id));
    }

    /** Escopo pela concessionária que executou o serviço (fora do escopo → 404). */
    private Servico buscarNoEscopo(Long id) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(RECURSO, id));
        if (!escopo.podeAcessar(servico.getConcessionaria().getId())) {
            throw new RecursoNaoEncontradoException(RECURSO, id);
        }
        return servico;
    }

    private Concessionaria referenciarConcessionaria(Long concessionariaId) {
        escopo.exigirAcessoParaEscrita(concessionariaId);
        return concessionariaRepository.findById(concessionariaId)
                .orElseThrow(() -> new RegraNegocioException(
                        "Concessionária %d não existe".formatted(concessionariaId)));
    }

    private void validar(ServicoRequest request, Veiculo veiculo) {
        if (request.data().isAfter(LocalDate.now(clock))) {
            throw new RegraNegocioException("A data do serviço não pode ser futura");
        }
        if (request.pago() && !request.tipo().podeSerPago()) {
            throw new RegraNegocioException(
                    "Serviços do tipo %s são custeados pela Ford e não podem ser registrados como pagos"
                            .formatted(request.tipo()));
        }
        if (request.tipo() == TipoServico.GARANTIA && !veiculo.getStatusGarantia().coberto()) {
            throw new RegraNegocioException("Veículo %s está com a garantia expirada".formatted(veiculo.getVin()));
        }
    }
}
