package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.PageResponse;
import br.com.fiap.fordretention.dto.cliente.ClienteFiltro;
import br.com.fiap.fordretention.dto.cliente.ClientePatchRequest;
import br.com.fiap.fordretention.dto.cliente.ClienteRequest;
import br.com.fiap.fordretention.dto.cliente.ClienteResponse;
import br.com.fiap.fordretention.exception.ConflitoException;
import br.com.fiap.fordretention.exception.RecursoNaoEncontradoException;
import br.com.fiap.fordretention.exception.RegraNegocioException;
import br.com.fiap.fordretention.mapper.ClienteMapper;
import br.com.fiap.fordretention.model.Cliente;
import br.com.fiap.fordretention.model.Concessionaria;
import br.com.fiap.fordretention.repository.ClienteRepository;
import br.com.fiap.fordretention.repository.ConcessionariaRepository;
import br.com.fiap.fordretention.repository.LeadRepository;
import br.com.fiap.fordretention.repository.VeiculoRepository;
import br.com.fiap.fordretention.repository.spec.ClienteSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class ClienteService {

    private static final String RECURSO = "Cliente";

    private final ClienteRepository clienteRepository;
    private final ConcessionariaRepository concessionariaRepository;
    private final VeiculoRepository veiculoRepository;
    private final LeadRepository leadRepository;
    private final ClienteMapper mapper;
    private final EscopoAcessoService escopo;

    public ClienteService(ClienteRepository clienteRepository, ConcessionariaRepository concessionariaRepository,
                          VeiculoRepository veiculoRepository, LeadRepository leadRepository,
                          ClienteMapper mapper, EscopoAcessoService escopo) {
        this.clienteRepository = clienteRepository;
        this.concessionariaRepository = concessionariaRepository;
        this.veiculoRepository = veiculoRepository;
        this.leadRepository = leadRepository;
        this.mapper = mapper;
        this.escopo = escopo;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClienteResponse> listar(ClienteFiltro filtro, Pageable pageable) {
        Long concessionariaId = escopo.resolverFiltro(filtro == null ? null : filtro.concessionariaId());
        var spec = ClienteSpecifications.filtrar(filtro, concessionariaId);
        return PageResponse.of(clienteRepository.findAll(spec, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Long id) {
        return mapper.toResponse(buscarNoEscopo(id));
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        Concessionaria concessionaria = referenciarConcessionaria(request.concessionariaPreferidaId());
        String email = normalizarEmail(request.email());
        if (clienteRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe um cliente com o e-mail " + email);
        }
        Cliente cliente = new Cliente(request.nome().trim(), email, request.telefone(), concessionaria);
        return mapper.toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarNoEscopo(id);
        Concessionaria concessionaria = referenciarConcessionaria(request.concessionariaPreferidaId());
        String email = normalizarEmail(request.email());
        validarEmailUnico(email, id);
        cliente.setNome(request.nome().trim());
        cliente.setEmail(email);
        cliente.setTelefone(request.telefone());
        cliente.setConcessionariaPreferida(concessionaria);
        return mapper.toResponse(cliente);
    }

    @Transactional
    public ClienteResponse atualizarParcial(Long id, ClientePatchRequest request) {
        Cliente cliente = buscarNoEscopo(id);
        if (request.nome() != null) {
            cliente.setNome(request.nome().trim());
        }
        if (request.email() != null) {
            String email = normalizarEmail(request.email());
            validarEmailUnico(email, id);
            cliente.setEmail(email);
        }
        if (request.telefone() != null) {
            cliente.setTelefone(request.telefone());
        }
        if (request.concessionariaPreferidaId() != null) {
            cliente.setConcessionariaPreferida(referenciarConcessionaria(request.concessionariaPreferidaId()));
        }
        return mapper.toResponse(cliente);
    }

    @Transactional
    public void excluir(Long id) {
        Cliente cliente = buscarNoEscopo(id);
        if (veiculoRepository.existsByClienteId(id) || leadRepository.existsByClienteId(id)) {
            throw new ConflitoException("Cliente possui veículos ou leads vinculados e não pode ser excluído");
        }
        clienteRepository.delete(cliente);
    }

    /** Busca para leitura/alteração do próprio recurso: fora do escopo → 404. */
    @Transactional(readOnly = true)
    public Cliente buscarNoEscopo(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(RECURSO, id));
        if (!escopo.podeAcessar(cliente.getConcessionariaPreferidaId())) {
            throw new RecursoNaoEncontradoException(RECURSO, id);
        }
        return cliente;
    }

    /** Cliente referenciado no corpo de outra requisição: inexistente → 422; de outra concessionária → 403. */
    @Transactional(readOnly = true)
    public Cliente buscarReferencia(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Cliente %d não existe".formatted(id)));
        escopo.exigirAcessoParaEscrita(cliente.getConcessionariaPreferidaId());
        return cliente;
    }

    private Concessionaria referenciarConcessionaria(Long concessionariaId) {
        escopo.exigirAcessoParaEscrita(concessionariaId);
        return concessionariaRepository.findById(concessionariaId)
                .orElseThrow(() -> new RegraNegocioException(
                        "Concessionária %d não existe".formatted(concessionariaId)));
    }

    private void validarEmailUnico(String email, Long id) {
        if (clienteRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflitoException("Já existe outro cliente com o e-mail " + email);
        }
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
