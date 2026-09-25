package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.config.LeadProperties;
import br.com.fiap.fordretention.dto.cliente.PerfilRequest;
import br.com.fiap.fordretention.dto.cliente.PerfilResponse;
import br.com.fiap.fordretention.exception.RecursoNaoEncontradoException;
import br.com.fiap.fordretention.mapper.ClienteMapper;
import br.com.fiap.fordretention.model.Cliente;
import br.com.fiap.fordretention.model.enums.PerfilCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilClienteServiceTest {

    @Mock
    private ClienteService clienteService;
    @Mock
    private LeadService leadService;

    private PerfilClienteService service;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        service = new PerfilClienteService(clienteService, leadService, new ClienteMapper(),
                new LeadProperties(new BigDecimal("0.70")), Fixtures.CLOCK);
        cliente = Fixtures.cliente(10, Fixtures.concessionaria(1), null, null);
    }

    @ParameterizedTest(name = "{0} com score {1} gera leads")
    @CsvSource({"ABANDONO, 0.91", "ESQUECIDO, 0.70", "ABANDONO, 1.0"})
    void geraLeadsQuandoPerfilDeRiscoAcimaDoLimite(PerfilCliente perfil, BigDecimal score) {
        when(clienteService.buscarNoEscopo(10L)).thenReturn(cliente);
        when(leadService.gerarLeadsAutomaticos(cliente)).thenReturn(List.of(1L, 2L));

        PerfilResponse resposta = service.atualizar(10L, new PerfilRequest(perfil, score));

        assertThat(resposta.leadsGerados()).containsExactly(1L, 2L);
        assertThat(cliente.getPerfil()).isEqualTo(perfil);
        assertThat(cliente.getScoreRisco()).isEqualByComparingTo(score);
        assertThat(cliente.getDataUltimaAtualizacaoPerfil()).isEqualTo(LocalDateTime.now(Fixtures.CLOCK));
    }

    @ParameterizedTest(name = "{0} com score {1} NÃO gera leads")
    @CsvSource({"ABANDONO, 0.69", "ESQUECIDO, 0.1", "FIEL, 0.99", "ECONOMICO, 0.95"})
    void naoGeraLeads(PerfilCliente perfil, BigDecimal score) {
        when(clienteService.buscarNoEscopo(10L)).thenReturn(cliente);

        PerfilResponse resposta = service.atualizar(10L, new PerfilRequest(perfil, score));

        assertThat(resposta.leadsGerados()).isEmpty();
        assertThat(resposta.perfil()).isEqualTo(perfil);
        verify(leadService, never()).gerarLeadsAutomaticos(any());
    }

    @Test
    @DisplayName("score é normalizado para 4 casas decimais")
    void normalizaScore() {
        when(clienteService.buscarNoEscopo(10L)).thenReturn(cliente);

        service.atualizar(10L, new PerfilRequest(PerfilCliente.FIEL, new BigDecimal("0.123456")));

        assertThat(cliente.getScoreRisco()).isEqualTo(new BigDecimal("0.1235"));
    }

    @Test
    @DisplayName("cliente sem perfil calculado → 404")
    void buscarSemPerfil() {
        when(clienteService.buscarNoEscopo(10L)).thenReturn(cliente);

        assertThatThrownBy(() -> service.buscar(10L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("ainda não possui perfil");
    }

    @Test
    void buscarComPerfil() {
        cliente.setPerfil(PerfilCliente.ECONOMICO);
        cliente.setScoreRisco(new BigDecimal("0.5500"));
        when(clienteService.buscarNoEscopo(10L)).thenReturn(cliente);

        PerfilResponse resposta = service.buscar(10L);

        assertThat(resposta.perfilNome()).isEqualTo("Cliente Econômico");
        assertThat(resposta.leadsGerados()).isEmpty();
    }
}
