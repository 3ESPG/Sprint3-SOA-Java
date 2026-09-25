package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.auth.RegisterRequest;
import br.com.fiap.fordretention.dto.usuario.UsuarioRequest;
import br.com.fiap.fordretention.dto.usuario.UsuarioResponse;
import br.com.fiap.fordretention.exception.ConflitoException;
import br.com.fiap.fordretention.exception.RegraNegocioException;
import br.com.fiap.fordretention.mapper.UsuarioMapper;
import br.com.fiap.fordretention.model.Usuario;
import br.com.fiap.fordretention.model.enums.Role;
import br.com.fiap.fordretention.repository.ConcessionariaRepository;
import br.com.fiap.fordretention.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ConcessionariaRepository concessionariaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EscopoAcessoService escopo;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, concessionariaRepository, passwordEncoder,
                new UsuarioMapper(), escopo);
    }

    @Test
    @DisplayName("auto-cadastro cria CONSULTOR inativo com senha em BCrypt e e-mail normalizado")
    void registrar() {
        when(concessionariaRepository.findById(1L)).thenReturn(Optional.of(Fixtures.concessionaria(1)));
        when(usuarioRepository.existsByEmailIgnoreCase("novo@ford.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha1234")).thenReturn("$2a$10$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse r = service.registrar(new RegisterRequest("Novo", " Novo@Ford.com ", "Senha1234", 1L));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getSenha()).isEqualTo("$2a$10$hash");
        assertThat(r.role()).isEqualTo(Role.CONSULTOR);
        assertThat(r.ativo()).isFalse();
        assertThat(r.email()).isEqualTo("novo@ford.com");
        assertThat(r.concessionariaId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("e-mail já cadastrado → 409")
    void emailDuplicado() {
        when(concessionariaRepository.findById(1L)).thenReturn(Optional.of(Fixtures.concessionaria(1)));
        when(usuarioRepository.existsByEmailIgnoreCase("admin@ford.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(new RegisterRequest("X", "admin@ford.com", "Senha1234", 1L)))
                .isInstanceOf(ConflitoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("concessionária inexistente → 422")
    void concessionariaInexistente() {
        when(concessionariaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(new RegisterRequest("X", "x@ford.com", "Senha1234", 9L)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("ADMIN não pode criar gestor sem concessionária → 422")
    void gestorSemConcessionaria() {
        var request = new UsuarioRequest("G", "g@ford.com", "Senha1234", Role.GESTOR_CONCESSIONARIA, null);

        assertThatThrownBy(() -> service.criar(request)).isInstanceOf(RegraNegocioException.class);
    }
}
