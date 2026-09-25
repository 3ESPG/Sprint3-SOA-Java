package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.PageResponse;
import br.com.fiap.fordretention.dto.concessionaria.ConcessionariaFiltro;
import br.com.fiap.fordretention.dto.concessionaria.ConcessionariaPatchRequest;
import br.com.fiap.fordretention.dto.concessionaria.ConcessionariaRequest;
import br.com.fiap.fordretention.dto.concessionaria.ConcessionariaResponse;
import br.com.fiap.fordretention.exception.ConflitoException;
import br.com.fiap.fordretention.exception.RecursoNaoEncontradoException;
import br.com.fiap.fordretention.mapper.ConcessionariaMapper;
import br.com.fiap.fordretention.model.Concessionaria;
import br.com.fiap.fordretention.repository.ClienteRepository;
import br.com.fiap.fordretention.repository.ConcessionariaRepository;
import br.com.fiap.fordretention.repository.LeadRepository;
import br.com.fiap.fordretention.repository.ServicoRepository;
import br.com.fiap.fordretention.repository.UsuarioRepository;
import br.com.fiap.fordretention.repository.spec.ConcessionariaSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConcessionariaService {

    private static final String RECURSO = "Concessionária";

    private final ConcessionariaRepository concessionariaRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final LeadRepository leadRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConcessionariaMapper mapper;
    private final EscopoAcessoService escopo;

    public ConcessionariaService(ConcessionariaRepository concessionariaRepository,
                                 ClienteRepository clienteRepository,
                                 ServicoRepository servicoRepository,
                                 LeadRepository leadRepository,
                                 UsuarioRepository usuarioRepository,
                                 ConcessionariaMapper mapper,
                                 EscopoAcessoService escopo) {
        this.concessionariaRepository = concessionariaRepository;
        this.clienteRepository = clienteRepository;
        this.servicoRepository = servicoRepository;
        this.leadRepository = leadRepository;
        this.usuarioRepository = usuarioRepository;
        this.mapper = mapper;
        this.escopo = escopo;
    }

    @Transactional(readOnly = true)
    public PageResponse<ConcessionariaResponse> listar(ConcessionariaFiltro filtro, Pageable pageable) {
        var spec = ConcessionariaSpecifications.filtrar(filtro, escopo.concessionariaRestrita());
        return PageResponse.of(concessionariaRepository.findAll(spec, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ConcessionariaResponse buscar(Long id) {
        return mapper.toResponse(buscarNoEscopo(id));
    }

    @Transactional
    public ConcessionariaResponse criar(ConcessionariaRequest request) {
        String cnpj = ConcessionariaMapper.normalizarCnpj(request.cnpj());
        if (concessionariaRepository.existsByCnpj(cnpj)) {
            throw new ConflitoException("Já existe uma concessionária com o CNPJ " + cnpj);
        }
        return mapper.toResponse(concessionariaRepository.save(mapper.toEntity(request)));
    }

    @Transactional
    public ConcessionariaResponse atualizar(Long id, ConcessionariaRequest request) {
        Concessionaria concessionaria = buscarEntidade(id);
        String cnpj = ConcessionariaMapper.normalizarCnpj(request.cnpj());
        if (concessionariaRepository.existsByCnpjAndIdNot(cnpj, id)) {
            throw new ConflitoException("Já existe outra concessionária com o CNPJ " + cnpj);
        }
        mapper.atualizar(concessionaria, request);
        return mapper.toResponse(concessionaria);
    }

    @Transactional
    public ConcessionariaResponse atualizarParcial(Long id, ConcessionariaPatchRequest request) {
        Concessionaria concessionaria = buscarEntidade(id);
        mapper.aplicarPatch(concessionaria, request);
        return mapper.toResponse(concessionaria);
    }

    @Transactional
    public void excluir(Long id) {
        Concessionaria concessionaria = buscarEntidade(id);
        if (clienteRepository.existsByConcessionariaPreferida_Id(id)
                || servicoRepository.existsByConcessionariaId(id)
                || leadRepository.existsByConcessionariaId(id)
                || usuarioRepository.existsByConcessionaria_Id(id)) {
            throw new ConflitoException(
                    "Concessionária possui clientes, serviços, leads ou usuários vinculados e não pode ser excluída");
        }
        concessionariaRepository.delete(concessionaria);
    }

    /** Busca respeitando o escopo do usuário (fora do escopo → 404). */
    @Transactional(readOnly = true)
    public Concessionaria buscarNoEscopo(Long id) {
        if (!escopo.podeAcessar(id)) {
            throw new RecursoNaoEncontradoException(RECURSO, id);
        }
        return buscarEntidade(id);
    }

    private Concessionaria buscarEntidade(Long id) {
        return concessionariaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(RECURSO, id));
    }
}
