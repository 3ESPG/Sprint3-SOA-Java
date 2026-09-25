package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.veiculo.VeiculoFiltro;
import br.com.fiap.fordretention.dto.veiculo.VeiculoPatchRequest;
import br.com.fiap.fordretention.dto.veiculo.VeiculoRequest;
import br.com.fiap.fordretention.dto.veiculo.VeiculoResponse;
import br.com.fiap.fordretention.exception.ConflitoException;
import br.com.fiap.fordretention.exception.RegraNegocioException;
import br.com.fiap.fordretention.mapper.VeiculoMapper;
import br.com.fiap.fordretention.model.Cliente;
import br.com.fiap.fordretention.model.Veiculo;
import br.com.fiap.fordretention.model.enums.StatusGarantia;
import br.com.fiap.fordretention.repository.LeadRepository;
import br.com.fiap.fordretention.repository.ServicoRepository;
import br.com.fiap.fordretention.repository.VeiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VeiculoServiceTest {

    @Mock
    private VeiculoRepository veiculoRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private LeadRepository leadRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private EscopoAcessoService escopo;

    private VeiculoService service;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        service = new VeiculoService(veiculoRepository, servicoRepository, leadRepository, clienteService,
                new VeiculoMapper(), escopo, Fixtures.CLOCK);
        cliente = Fixtures.cliente(10, Fixtures.concessionaria(1), null, null);
    }

    @Test
    @DisplayName("cria veículo normalizando o VIN e calculando a idade")
    void criar() {
        when(clienteService.buscarReferencia(10L)).thenReturn(cliente);
        when(veiculoRepository.existsByVin("9BFZZZ55LA0000099")).thenReturn(false);
        when(veiculoRepository.save(any(Veiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        VeiculoResponse r = service.criar(new VeiculoRequest("9bfzzz55la0000099", "Ranger", 2019, 90_000, 10L,
                StatusGarantia.EXPIRADA));

        assertThat(r.vin()).isEqualTo("9BFZZZ55LA0000099");
        assertThat(r.idade()).isEqualTo(7);
    }

    @Test
    @DisplayName("VIN duplicado → 409")
    void vinDuplicado() {
        when(clienteService.buscarReferencia(10L)).thenReturn(cliente);
        when(veiculoRepository.existsByVin("9BFZZZ55LA0000099")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new VeiculoRequest("9BFZZZ55LA0000099", "Ranger", 2019, 1, 10L,
                StatusGarantia.ATIVA))).isInstanceOf(ConflitoException.class);
        verify(veiculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("ano posterior ao próximo ano-modelo → 422")
    void anoFuturo() {
        when(clienteService.buscarReferencia(10L)).thenReturn(cliente);

        assertThatThrownBy(() -> service.criar(new VeiculoRequest("9BFZZZ55LA0000099", "Ranger", 2030, 0, 10L,
                StatusGarantia.ATIVA)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("2027");
    }

    @Test
    @DisplayName("PATCH não aceita quilometragem menor que a atual → 422")
    void quilometragemNaoDiminui() {
        Veiculo veiculo = Fixtures.veiculo(1, cliente, "Ka", 2019);
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
        when(escopo.podeAcessar(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizarParcial(1L, new VeiculoPatchRequest(10, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não pode diminuir");
        assertThat(veiculo.getQuilometragem()).isEqualTo(50_000);
    }

    @Test
    void patchAtualizaQuilometragemEGarantia() {
        Veiculo veiculo = Fixtures.veiculo(1, cliente, "Ka", 2019);
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
        when(escopo.podeAcessar(1L)).thenReturn(true);

        VeiculoResponse r = service.atualizarParcial(1L, new VeiculoPatchRequest(61_000, StatusGarantia.ESTENDIDA));

        assertThat(r.quilometragem()).isEqualTo(61_000);
        assertThat(r.statusGarantia()).isEqualTo(StatusGarantia.ESTENDIDA);
    }

    @Test
    @DisplayName("filtro com idadeMin > idadeMax → 422")
    void filtroIdadeInconsistente() {
        var filtro = new VeiculoFiltro(null, 8, 4, null, null, null);

        assertThatThrownBy(() -> service.listar(filtro, Pageable.unpaged()))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("não exclui veículo com histórico de serviços → 409")
    void excluirComHistorico() {
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(Fixtures.veiculo(1, cliente, "Ka", 2019)));
        when(escopo.podeAcessar(1L)).thenReturn(true);
        when(servicoRepository.existsByVeiculoId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(1L)).isInstanceOf(ConflitoException.class);
    }
}
