package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.servico.ServicoRequest;
import br.com.fiap.fordretention.dto.servico.ServicoResponse;
import br.com.fiap.fordretention.exception.RegraNegocioException;
import br.com.fiap.fordretention.mapper.ServicoMapper;
import br.com.fiap.fordretention.model.Concessionaria;
import br.com.fiap.fordretention.model.Servico;
import br.com.fiap.fordretention.model.Veiculo;
import br.com.fiap.fordretention.model.enums.TipoServico;
import br.com.fiap.fordretention.repository.ConcessionariaRepository;
import br.com.fiap.fordretention.repository.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 24);

    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private ConcessionariaRepository concessionariaRepository;
    @Mock
    private VeiculoService veiculoService;
    @Mock
    private LeadService leadService;
    @Mock
    private EscopoAcessoService escopo;

    private ServicoService service;
    private Concessionaria concessionaria;
    private Veiculo veiculo;

    @BeforeEach
    void setUp() {
        service = new ServicoService(servicoRepository, concessionariaRepository, veiculoService, leadService,
                new ServicoMapper(), escopo, Fixtures.CLOCK);
        concessionaria = Fixtures.concessionaria(1);
        veiculo = Fixtures.veiculo(7, Fixtures.cliente(1, concessionaria, null, null), "Ranger", 2019);
    }

    private ServicoRequest request(TipoServico tipo, LocalDate data, boolean pago) {
        return new ServicoRequest(7L, 1L, tipo, new BigDecimal("890.00"), data, pago, null);
    }

    private void referenciasValidas() {
        when(concessionariaRepository.findById(1L)).thenReturn(Optional.of(concessionaria));
        when(veiculoService.buscarReferencia(7L)).thenReturn(veiculo);
    }

    @Test
    @DisplayName("serviço pago é registrado e converte os leads ativos do veículo")
    void criarPagoConverteLeads() {
        referenciasValidas();
        when(servicoRepository.save(any(Servico.class))).thenAnswer(inv -> {
            Servico s = inv.getArgument(0);
            s.setId(50L);
            return s;
        });

        ServicoResponse r = service.criar(request(TipoServico.REVISAO, HOJE, true));

        assertThat(r.id()).isEqualTo(50L);
        assertThat(r.pago()).isTrue();
        verify(leadService).converterLeadsAtivos(7L, 50L);
    }

    @Test
    @DisplayName("serviço não pago não mexe nos leads")
    void criarNaoPago() {
        referenciasValidas();
        when(servicoRepository.save(any(Servico.class))).thenAnswer(inv -> inv.getArgument(0));

        service.criar(request(TipoServico.REPARO, HOJE, false));

        verifyNoInteractions(leadService);
    }

    @Test
    @DisplayName("data futura → 422")
    void dataFutura() {
        referenciasValidas();

        assertThatThrownBy(() -> service.criar(request(TipoServico.REVISAO, HOJE.plusDays(1), true)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("futura");
        verify(servicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RECALL marcado como pago → 422")
    void recallPago() {
        referenciasValidas();

        assertThatThrownBy(() -> service.criar(request(TipoServico.RECALL, HOJE, true)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("RECALL");
    }

    @Test
    @DisplayName("GARANTIA em veículo com garantia expirada → 422")
    void garantiaExpirada() {
        referenciasValidas();

        assertThatThrownBy(() -> service.criar(request(TipoServico.GARANTIA, HOJE, false)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("garantia expirada");
    }

    @Test
    @DisplayName("gestor registrando serviço em outra concessionária → 403")
    void outraConcessionaria() {
        doThrow(new AccessDeniedException("fora do escopo")).when(escopo).exigirAcessoParaEscrita(1L);

        assertThatThrownBy(() -> service.criar(request(TipoServico.REVISAO, HOJE, true)))
                .isInstanceOf(AccessDeniedException.class);
        verify(concessionariaRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("concessionária inexistente no payload → 422")
    void concessionariaInexistente() {
        when(concessionariaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(request(TipoServico.REVISAO, HOJE, true)))
                .isInstanceOf(RegraNegocioException.class);
    }
}
