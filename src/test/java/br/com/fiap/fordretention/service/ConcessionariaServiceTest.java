package br.com.fiap.fordretention.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConcessionariaServiceTest {

    @Mock
    private ConcessionariaRepository concessionariaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private LeadRepository leadRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EscopoAcessoService escopo;

    private ConcessionariaService service;

    @BeforeEach
    void setUp() {
        service = new ConcessionariaService(concessionariaRepository, clienteRepository, servicoRepository,
                leadRepository, usuarioRepository, new ConcessionariaMapper(), escopo);
    }

    @Test
    @DisplayName("cria normalizando CNPJ (só dígitos) e UF (maiúscula)")
    void criaNormalizando() {
        when(concessionariaRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(concessionariaRepository.save(any(Concessionaria.class))).thenAnswer(inv -> {
            Concessionaria c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });

        ConcessionariaResponse r = service.criar(
                new ConcessionariaRequest(" Ford Nova ", "Campinas", "sp", "11.222.333/0001-81"));

        assertThat(r.id()).isEqualTo(5L);
        assertThat(r.nome()).isEqualTo("Ford Nova");
        assertThat(r.estado()).isEqualTo("SP");
        assertThat(r.cnpj()).isEqualTo("11222333000181");
    }

    @Test
    @DisplayName("CNPJ duplicado → 409")
    void cnpjDuplicado() {
        when(concessionariaRepository.existsByCnpj("11222333000181")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(
                new ConcessionariaRequest("Ford", "SP", "SP", "11.222.333/0001-81")))
                .isInstanceOf(ConflitoException.class);
        verify(concessionariaRepository, never()).save(any());
    }

    @Test
    @DisplayName("gestor pedindo outra concessionária → 404 sem consultar o banco")
    void buscarForaDoEscopo() {
        when(escopo.podeAcessar(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.buscar(2L)).isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(concessionariaRepository);
    }

    @Test
    void buscarInexistente() {
        when(escopo.podeAcessar(9L)).thenReturn(true);
        when(concessionariaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("PATCH altera apenas os campos informados")
    void patchParcial() {
        Concessionaria existente = Fixtures.concessionaria(1);
        when(concessionariaRepository.findById(1L)).thenReturn(Optional.of(existente));

        ConcessionariaResponse r = service.atualizarParcial(1L, new ConcessionariaPatchRequest(null, "Santos", null));

        assertThat(r.cidade()).isEqualTo("Santos");
        assertThat(r.nome()).isEqualTo("Ford 1");
        assertThat(r.estado()).isEqualTo("SP");
    }

    @Test
    @DisplayName("não exclui concessionária com clientes vinculados → 409")
    void excluirComVinculos() {
        when(concessionariaRepository.findById(1L)).thenReturn(Optional.of(Fixtures.concessionaria(1)));
        when(clienteRepository.existsByConcessionariaPreferida_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(1L)).isInstanceOf(ConflitoException.class);
        verify(concessionariaRepository, never()).delete(any(Concessionaria.class));
    }

    @Test
    void excluirSemVinculos() {
        Concessionaria c = Fixtures.concessionaria(1);
        when(concessionariaRepository.findById(1L)).thenReturn(Optional.of(c));

        service.excluir(1L);

        verify(concessionariaRepository).delete(c);
    }
}
