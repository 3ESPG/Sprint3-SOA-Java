package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.PageResponse;
import br.com.fiap.fordretention.dto.lead.LeadFiltro;
import br.com.fiap.fordretention.dto.lead.LeadRequest;
import br.com.fiap.fordretention.dto.lead.LeadResponse;
import br.com.fiap.fordretention.dto.lead.LeadStatusRequest;
import br.com.fiap.fordretention.exception.ConflitoException;
import br.com.fiap.fordretention.exception.RecursoNaoEncontradoException;
import br.com.fiap.fordretention.exception.RegraNegocioException;
import br.com.fiap.fordretention.mapper.LeadMapper;
import br.com.fiap.fordretention.model.Cliente;
import br.com.fiap.fordretention.model.Lead;
import br.com.fiap.fordretention.model.Veiculo;
import br.com.fiap.fordretention.model.enums.OrigemLead;
import br.com.fiap.fordretention.model.enums.PerfilCliente;
import br.com.fiap.fordretention.model.enums.Prioridade;
import br.com.fiap.fordretention.model.enums.StatusLead;
import br.com.fiap.fordretention.repository.LeadRepository;
import br.com.fiap.fordretention.repository.VeiculoRepository;
import br.com.fiap.fordretention.repository.spec.LeadSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class LeadService {

    private static final Logger log = LoggerFactory.getLogger(LeadService.class);

    private static final String RECURSO = "Lead";

    private final LeadRepository leadRepository;
    private final VeiculoRepository veiculoRepository;
    private final ClienteService clienteService;
    private final LeadMapper mapper;
    private final EscopoAcessoService escopo;
    private final Clock clock;

    public LeadService(LeadRepository leadRepository, VeiculoRepository veiculoRepository,
                       ClienteService clienteService, LeadMapper mapper, EscopoAcessoService escopo, Clock clock) {
        this.leadRepository = leadRepository;
        this.veiculoRepository = veiculoRepository;
        this.clienteService = clienteService;
        this.mapper = mapper;
        this.escopo = escopo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> listar(LeadFiltro filtro, Pageable pageable) {
        Long concessionariaId = escopo.resolverFiltro(filtro == null ? null : filtro.concessionariaId());
        var spec = LeadSpecifications.filtrar(filtro, concessionariaId);
        return PageResponse.of(leadRepository.findAll(spec, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public LeadResponse buscar(Long id) {
        return mapper.toResponse(buscarNoEscopo(id));
    }

    /** Lead manual. A concessionária responsável é a concessionária preferida do cliente. */
    @Transactional
    public LeadResponse criar(LeadRequest request) {
        Cliente cliente = clienteService.buscarReferencia(request.clienteId());
        Veiculo veiculo = buscarVeiculoDoCliente(request.veiculoId(), cliente);
        if (leadRepository.existsByVeiculoIdAndStatusIn(veiculo.getId(), StatusLead.ativos())) {
            throw new ConflitoException("Já existe um lead ativo para o veículo " + veiculo.getVin());
        }
        Lead lead = new Lead(cliente, veiculo, cliente.getConcessionariaPreferida(), request.motivo().trim(),
                request.prioridade(), OrigemLead.MANUAL, agora());
        return mapper.toResponse(leadRepository.save(lead));
    }

    @Transactional
    public LeadResponse atualizar(Long id, LeadRequest request) {
        Lead lead = buscarNoEscopo(id);
        if (!lead.getStatus().ativo()) {
            throw new RegraNegocioException("Lead com status %s está finalizado e não pode ser alterado"
                    .formatted(lead.getStatus()));
        }
        Cliente cliente = clienteService.buscarReferencia(request.clienteId());
        Veiculo veiculo = buscarVeiculoDoCliente(request.veiculoId(), cliente);
        if (leadRepository.existsByVeiculoIdAndStatusInAndIdNot(veiculo.getId(), StatusLead.ativos(), id)) {
            throw new ConflitoException("Já existe outro lead ativo para o veículo " + veiculo.getVin());
        }
        lead.setCliente(cliente);
        lead.setVeiculo(veiculo);
        lead.setConcessionaria(cliente.getConcessionariaPreferida());
        lead.setMotivo(request.motivo().trim());
        lead.setPrioridade(request.prioridade());
        lead.setDataAtualizacao(agora());
        return mapper.toResponse(lead);
    }

    /** Avança o lead no funil respeitando a máquina de estados de {@link StatusLead}. */
    @Transactional
    public LeadResponse alterarStatus(Long id, LeadStatusRequest request) {
        Lead lead = buscarNoEscopo(id);
        StatusLead atual = lead.getStatus();
        if (!atual.podeIrPara(request.status())) {
            throw new RegraNegocioException("Transição de %s para %s não é permitida. Próximos status válidos: %s"
                    .formatted(atual, request.status(), atual.proximosPermitidos()));
        }
        lead.setStatus(request.status());
        if (request.observacao() != null && !request.observacao().isBlank()) {
            lead.setObservacao(request.observacao().trim());
        }
        lead.setDataAtualizacao(agora());
        log.atInfo()
                .addKeyValue("evento", "lead.status_alterado")
                .addKeyValue("leadId", lead.getId())
                .addKeyValue("de", atual)
                .addKeyValue("para", request.status())
                .log("Status do lead alterado");
        return mapper.toResponse(lead);
    }

    @Transactional
    public void excluir(Long id) {
        leadRepository.delete(buscarNoEscopo(id));
    }

    /**
     * Regra do ML: cria um lead (origem MODELO_ML) para cada veículo do cliente que ainda não possui lead ativo.
     *
     * @return ids dos leads criados
     */
    @Transactional
    public List<Long> gerarLeadsAutomaticos(Cliente cliente) {
        int anoAtual = LocalDateTime.now(clock).getYear();
        Prioridade prioridade = Prioridade.porScore(cliente.getScoreRisco());
        List<Long> gerados = new ArrayList<>();
        for (Veiculo veiculo : veiculoRepository.findByClienteId(cliente.getId())) {
            if (leadRepository.existsByVeiculoIdAndStatusIn(veiculo.getId(), StatusLead.ativos())) {
                continue;
            }
            Lead lead = new Lead(cliente, veiculo, cliente.getConcessionariaPreferida(),
                    motivoAutomatico(cliente, veiculo, anoAtual), prioridade, OrigemLead.MODELO_ML, agora());
            gerados.add(leadRepository.save(lead).getId());
        }
        return gerados;
    }

    /** Um serviço pago registrado para o veículo converte os leads ativos dele. */
    @Transactional
    public int converterLeadsAtivos(Long veiculoId, Long servicoId) {
        List<Lead> ativos = leadRepository.findByVeiculoIdAndStatusIn(veiculoId, StatusLead.ativos());
        LocalDateTime agora = agora();
        for (Lead lead : ativos) {
            lead.setStatus(StatusLead.CONVERTIDO);
            lead.setObservacao("Convertido automaticamente pelo serviço pago #" + servicoId);
            lead.setDataAtualizacao(agora);
        }
        return ativos.size();
    }

    static String motivoAutomatico(Cliente cliente, Veiculo veiculo, int anoAtual) {
        PerfilCliente perfil = cliente.getPerfil();
        String acao = perfil == PerfilCliente.ABANDONO
                ? "alto risco de sair da rede Ford: oferecer revisão com condição especial"
                : "sem serviço recente: enviar lembrete de manutenção preventiva";
        return "Modelo de ML classificou o cliente como %s (score %s). %s %d (%d anos) — %s."
                .formatted(perfil.getNome(), cliente.getScoreRisco().stripTrailingZeros().toPlainString(),
                        veiculo.getModelo(), veiculo.getAno(), veiculo.idade(anoAtual), acao);
    }

    private Lead buscarNoEscopo(Long id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(RECURSO, id));
        if (!escopo.podeAcessar(lead.getConcessionaria().getId())) {
            throw new RecursoNaoEncontradoException(RECURSO, id);
        }
        return lead;
    }

    private Veiculo buscarVeiculoDoCliente(Long veiculoId, Cliente cliente) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new RegraNegocioException("Veículo %d não existe".formatted(veiculoId)));
        if (!Objects.equals(veiculo.getCliente().getId(), cliente.getId())) {
            throw new RegraNegocioException("Veículo %d não pertence ao cliente %d".formatted(veiculoId,
                    cliente.getId()));
        }
        return veiculo;
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
