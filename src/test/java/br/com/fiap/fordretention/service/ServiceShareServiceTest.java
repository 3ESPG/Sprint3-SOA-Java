package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.concessionaria.ServiceShareResponse;
import br.com.fiap.fordretention.dto.concessionaria.ServiceShareResponse.Segmento;
import br.com.fiap.fordretention.exception.RecursoNaoEncontradoException;
import br.com.fiap.fordretention.model.Cliente;
import br.com.fiap.fordretention.model.Concessionaria;
import br.com.fiap.fordretention.model.enums.TipoServico;
import br.com.fiap.fordretention.repository.ResumoServicoPorTipo;
import br.com.fiap.fordretention.repository.ServicoRepository;
import br.com.fiap.fordretention.repository.VeiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceShareServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 24);
    private static final LocalDate INICIO = LocalDate.of(2025, 9, 25);

    @Mock
    private ConcessionariaService concessionariaService;
    @Mock
    private VeiculoRepository veiculoRepository;
    @Mock
    private ServicoRepository servicoRepository;

    private ServiceShareService service;
    private Concessionaria concessionaria;

    @BeforeEach
    void setUp() {
        service = new ServiceShareService(concessionariaService, veiculoRepository, servicoRepository, Fixtures.CLOCK);
        concessionaria = Fixtures.concessionaria(1);
    }

    @Test
    @DisplayName("calcula Service Share por cliente e VIN Share por idade, modelo e tipo de serviço")
    void calculaIndicadores() {
        Cliente fiel = Fixtures.cliente(1, concessionaria, null, null);
        Cliente abandono = Fixtures.cliente(2, concessionaria, null, null);
        Cliente esquecido = Fixtures.cliente(3, concessionaria, null, null);
        var rangerNova = Fixtures.veiculo(10, fiel, "Ranger", 2024);      // 2 anos, com serviço
        var kaDoFiel = Fixtures.veiculo(11, fiel, "Ka", 2021);            // 5 anos, sem serviço
        var rangerAntiga = Fixtures.veiculo(12, abandono, "Ranger", 2018); // 8 anos, sem serviço
        var ecoSport = Fixtures.veiculo(13, esquecido, "EcoSport", 2020); // 6 anos, com serviço

        when(concessionariaService.buscarNoEscopo(1L)).thenReturn(concessionaria);
        when(veiculoRepository.findByCliente_ConcessionariaPreferida_Id(1L))
                .thenReturn(List.of(rangerNova, kaDoFiel, rangerAntiga, ecoSport));
        when(servicoRepository.findVeiculosComServicoPago(1L, INICIO, HOJE)).thenReturn(Set.of(10L, 13L));
        when(servicoRepository.resumirServicosPagosPorTipo(1L, INICIO, HOJE)).thenReturn(List.of(
                new ResumoServicoPorTipo(TipoServico.REVISAO, 2L, new BigDecimal("1500.00"))));

        ServiceShareResponse r = service.calcular(1L, 12);

        assertThat(r.periodoInicio()).isEqualTo(INICIO);
        assertThat(r.periodoFim()).isEqualTo(HOJE);
        assertThat(r.totalClientes()).isEqualTo(3);
        assertThat(r.clientesComServicoPago()).isEqualTo(2);
        assertThat(r.serviceShare()).isEqualByComparingTo("66.67");
        assertThat(r.totalVeiculos()).isEqualTo(4);
        assertThat(r.veiculosComServicoPago()).isEqualTo(2);
        assertThat(r.vinShare()).isEqualByComparingTo("50.00");
        assertThat(r.porIdadeVeiculo()).containsExactly(
                new Segmento("0-3 anos", 1, 1, new BigDecimal("100.00")),
                new Segmento("4-6 anos", 2, 1, new BigDecimal("50.00")),
                new Segmento("7+ anos", 1, 0, new BigDecimal("0.00")));
        assertThat(r.porModelo()).extracting(Segmento::segmento).containsExactly("Ranger", "EcoSport", "Ka");
        assertThat(r.porTipoServico()).hasSize(1);
        assertThat(r.porTipoServico().getFirst().receita()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("concessionária sem parque retorna zeros sem dividir por zero")
    void semVeiculos() {
        when(concessionariaService.buscarNoEscopo(1L)).thenReturn(concessionaria);
        when(veiculoRepository.findByCliente_ConcessionariaPreferida_Id(1L)).thenReturn(List.of());
        when(servicoRepository.findVeiculosComServicoPago(1L, INICIO, HOJE)).thenReturn(Set.of());
        when(servicoRepository.resumirServicosPagosPorTipo(1L, INICIO, HOJE)).thenReturn(List.of());

        ServiceShareResponse r = service.calcular(1L, 12);

        assertThat(r.serviceShare()).isEqualByComparingTo("0");
        assertThat(r.porIdadeVeiculo()).hasSize(3).allMatch(s -> s.total() == 0);
        assertThat(r.porModelo()).isEmpty();
    }

    @Test
    @DisplayName("fora do escopo do gestor → 404 e nada é consultado")
    void foraDoEscopo() {
        when(concessionariaService.buscarNoEscopo(2L)).thenThrow(new RecursoNaoEncontradoException("Concessionária", 2L));

        assertThatThrownBy(() -> service.calcular(2L, 12)).isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(veiculoRepository, servicoRepository);
    }

    @ParameterizedTest
    @CsvSource({"0, 0-3 anos", "3, 0-3 anos", "4, 4-6 anos", "6, 4-6 anos", "7, 7+ anos", "15, 7+ anos"})
    void faixaDeIdade(int idade, String faixa) {
        assertThat(ServiceShareService.faixaIdade(idade)).isEqualTo(faixa);
    }

    @Test
    void percentualArredondaDuasCasas() {
        assertThat(ServiceShareService.percentual(1, 3)).isEqualTo(new BigDecimal("33.33"));
        assertThat(ServiceShareService.percentual(0, 0)).isEqualTo(new BigDecimal("0.00"));
    }
}
