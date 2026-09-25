package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.PageResponse;
import br.com.fiap.fordretention.dto.auth.RegisterRequest;
import br.com.fiap.fordretention.dto.usuario.UsuarioRequest;
import br.com.fiap.fordretention.dto.usuario.UsuarioResponse;
import br.com.fiap.fordretention.exception.ConflitoException;
import br.com.fiap.fordretention.exception.RecursoNaoEncontradoException;
import br.com.fiap.fordretention.exception.RegraNegocioException;
import br.com.fiap.fordretention.mapper.UsuarioMapper;
import br.com.fiap.fordretention.model.Concessionaria;
import br.com.fiap.fordretention.model.Usuario;
import br.com.fiap.fordretention.model.enums.Role;
import br.com.fiap.fordretention.repository.ConcessionariaRepository;
import br.com.fiap.fordretention.repository.UsuarioRepository;
import br.com.fiap.fordretention.security.UsuarioAutenticado;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ConcessionariaRepository concessionariaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;
    private final EscopoAcessoService escopo;

    public UsuarioService(UsuarioRepository usuarioRepository, ConcessionariaRepository concessionariaRepository,
                          PasswordEncoder passwordEncoder, UsuarioMapper mapper, EscopoAcessoService escopo) {
        this.usuarioRepository = usuarioRepository;
        this.concessionariaRepository = concessionariaRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.escopo = escopo;
    }

    /** Auto-cadastro público: sempre CONSULTOR e inativo até aprovação de um ADMIN ou do gestor. */
    @Transactional
    public UsuarioResponse registrar(RegisterRequest request) {
        Concessionaria concessionaria = buscarConcessionariaReferenciada(request.concessionariaId());
        Usuario usuario = novoUsuario(request.nome(), request.email(), request.senha(), Role.CONSULTOR,
                concessionaria, false);
        return mapper.toResponse(usuarioRepository.save(usuario));
    }

    /** Criação por ADMIN, com qualquer role. */
    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        Concessionaria concessionaria = null;
        if (request.role() != Role.ADMIN) {
            if (request.concessionariaId() == null) {
                throw new RegraNegocioException("concessionariaId é obrigatório para o perfil " + request.role());
            }
            concessionaria = buscarConcessionariaReferenciada(request.concessionariaId());
        }
        Usuario usuario = novoUsuario(request.nome(), request.email(), request.senha(), request.role(),
                concessionaria, true);
        return mapper.toResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(Pageable pageable) {
        Long restrita = escopo.concessionariaRestrita();
        var pagina = restrita == null
                ? usuarioRepository.findAll(pageable)
                : usuarioRepository.findByConcessionaria_Id(restrita, pageable);
        return PageResponse.of(pagina.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return mapper.toResponse(buscarNoEscopo(id));
    }

    @Transactional
    public UsuarioResponse alterarAtivacao(Long id, boolean ativo) {
        Usuario usuario = buscarNoEscopo(id);
        UsuarioAutenticado atual = escopo.usuarioAtual();
        if (Objects.equals(usuario.getId(), atual.id())) {
            throw new RegraNegocioException("Não é possível alterar a ativação do próprio usuário");
        }
        if (!atual.isAdmin() && usuario.getRole() != Role.CONSULTOR) {
            throw new AccessDeniedException("Gestores só podem ativar/desativar consultores");
        }
        usuario.setAtivo(ativo);
        return mapper.toResponse(usuario);
    }

    /** ADMIN vê todos; GESTOR vê os usuários da própria concessionária; CONSULTOR só a si mesmo. */
    private Usuario buscarNoEscopo(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
        UsuarioAutenticado atual = escopo.usuarioAtual();
        boolean permitido = switch (atual.role()) {
            case ADMIN -> true;
            case GESTOR_CONCESSIONARIA -> Objects.equals(usuario.getConcessionariaId(), atual.concessionariaId());
            case CONSULTOR -> Objects.equals(usuario.getId(), atual.id());
        };
        if (!permitido) {
            throw new RecursoNaoEncontradoException("Usuário", id);
        }
        return usuario;
    }

    private Usuario novoUsuario(String nome, String email, String senha, Role role, Concessionaria concessionaria,
                                boolean ativo) {
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new ConflitoException("Já existe um usuário com o e-mail " + emailNormalizado);
        }
        return new Usuario(nome.trim(), emailNormalizado, passwordEncoder.encode(senha), role, concessionaria, ativo);
    }

    private Concessionaria buscarConcessionariaReferenciada(Long id) {
        return concessionariaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Concessionária %d não existe".formatted(id)));
    }
}
