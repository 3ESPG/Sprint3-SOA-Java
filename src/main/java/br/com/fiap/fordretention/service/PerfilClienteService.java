package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.config.LeadProperties;
import br.com.fiap.fordretention.dto.cliente.PerfilRequest;
import br.com.fiap.fordretention.dto.cliente.PerfilResponse;
import br.com.fiap.fordretention.exception.RecursoNaoEncontradoException;
import br.com.fiap.fordretention.mapper.ClienteMapper;
import br.com.fiap.fordretention.model.Cliente;
import br.com.fiap.fordretention.model.enums.PerfilCliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Integração com o modelo de ML: recebe o perfil/score calculado pelo notebook Python e, quando
 * o cliente é classificado como ABANDONO ou ESQUECIDO com score ≥ limite, gera leads automaticamente.
 */
@Service
public class PerfilClienteService {

    private static final Logger log = LoggerFactory.getLogger(PerfilClienteService.class);

    private final ClienteService clienteService;
    private final LeadService leadService;
    private final ClienteMapper mapper;
    private final LeadProperties leadProperties;
    private final Clock clock;

    public PerfilClienteService(ClienteService clienteService, LeadService leadService, ClienteMapper mapper,
                                LeadProperties leadProperties, Clock clock) {
        this.clienteService = clienteService;
        this.leadService = leadService;
        this.mapper = mapper;
        this.leadProperties = leadProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PerfilResponse buscar(Long clienteId) {
        Cliente cliente = clienteService.buscarNoEscopo(clienteId);
        if (cliente.getPerfil() == null) {
            throw new RecursoNaoEncontradoException(
                    "Cliente %d ainda não possui perfil calculado pelo modelo".formatted(clienteId));
        }
        return mapper.toPerfilResponse(cliente, List.of());
    }

    @Transactional
    public PerfilResponse atualizar(Long clienteId, PerfilRequest request) {
        Cliente cliente = clienteService.buscarNoEscopo(clienteId);
        BigDecimal score = request.scoreRisco().setScale(4, RoundingMode.HALF_UP);
        cliente.setPerfil(request.perfil());
        cliente.setScoreRisco(score);
        cliente.setDataUltimaAtualizacaoPerfil(LocalDateTime.now(clock));

        List<Long> leadsGerados = deveGerarLead(request.perfil(), score)
                ? leadService.gerarLeadsAutomaticos(cliente)
                : List.of();
        log.debug("Perfil do cliente {} atualizado para {} (score {}); leads gerados: {}",
                clienteId, request.perfil(), score, leadsGerados);
        return mapper.toPerfilResponse(cliente, leadsGerados);
    }

    boolean deveGerarLead(PerfilCliente perfil, BigDecimal score) {
        return perfil.geraLeadAutomatico() && score.compareTo(leadProperties.scoreLimite()) >= 0;
    }
}
