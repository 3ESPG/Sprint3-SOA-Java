package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.lead.LeadRequest;
import br.com.fiap.fordretention.dto.lead.LeadResponse;
import br.com.fiap.fordretention.dto.lead.LeadStatusRequest;
import br.com.fiap.fordretention.exception.ConflitoException;
import br.com.fiap.fordretention.exception.RecursoNaoEncontradoException;
import br.com.fiap.fordretention.exception.RegraNegocioException;
import br.com.fiap.fordretention.mapper.LeadMapper;
import br.com.fiap.fordretention.model.Cliente;
import br.com.fiap.fordretention.model.Concessionaria;
import br.com.fiap.fordretention.model.Lead;
import br.com.fiap.fordretention.model.Veiculo;
import br.com.fiap.fordretention.model.enums.OrigemLead;
import br.com.fiap.fordretention.model.enums.PerfilCliente;
import br.com.fiap.fordretention.model.enums.Prioridade;
import br.com.fiap.fordretention.model.enums.StatusLead;
import br.com.fiap.fordretention.repository.LeadRepository;
import br.com.fiap.fordretention.repository.VeiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private VeiculoRepository veiculoRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private EscopoAcessoService escopo;

    private LeadService service;
    private Concessionaria concessionaria;
    private Cliente cliente;
    private Veiculo veiculo;

    @BeforeEach
    void setUp() {
        service = new LeadService(leadRepository, veiculoRepository, clienteService, new LeadMapper(), escopo,
                Fixtures.CLOCK);
        concessionaria = Fixtures.concessionaria(1);
        cliente = Fixtures.cliente(10, concessionaria, PerfilCliente.ABANDONO, "0.9500");
        veiculo = Fixtures.veiculo(100, cliente, "Ranger", 2019);
    }

    @Test
    @DisplayName("gera um lead MODELO_ML por veículo que ainda não tem lead ativo")
    void gerarLeadsAutomaticos() {
        Veiculo comLeadAtivo = Fixtures.veiculo(101, cliente, "Ka", 2018);
        when(veiculoRepository.findByClienteId(10L)).thenReturn(List.of(veiculo, comLeadAtivo));
        when(leadRepository.existsByVeiculoIdAndStatusIn(100L, StatusLead.ativos())).thenReturn(false);
        when(leadRepository.existsByVeiculoIdAndStatusIn(101L, StatusLead.ativos())).thenReturn(true);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
            Lead lead = inv.getArgument(0);
            lead.setId(555L);
            return lead;
        });

        List<Long> ids = service.gerarLeadsAutomaticos(cliente);

        assertThat(ids).containsExactly(555L);
        ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository, times(1)).save(captor.capture());
        Lead criado = captor.getValue();
        assertThat(criado.getVeiculo()).isSameAs(veiculo);
        assertThat(criado.getConcessionaria()).isSameAs(concessionaria);
        assertThat(criado.getOrigem()).isEqualTo(OrigemLead.MODELO_ML);
        assertThat(criado.getStatus()).isEqualTo(StatusLead.ABERTO);
        assertThat(criado.getPrioridade()).isEqualTo(Prioridade.ALTA);
        assertThat(criado.getMotivo()).contains("Cliente de Abandono", "0.95", "Ranger 2019 (7 anos)");
        assertThat(criado.getDataCriacao()).isEqualTo(LocalDateTime.now(Fixtures.CLOCK));
    }

    @Test
    @DisplayName("avança o status quando a transição é permitida")
    void alterarStatusValido() {
        Lead lead = Fixtures.lead(1, cliente, veiculo);
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(escopo.podeAcessar(1L)).thenReturn(true);

        LeadResponse resposta = service.alterarStatus(1L, new LeadStatusRequest(StatusLead.CONTATADO, "Ligou"));

        assertThat(resposta.status()).isEqualTo(StatusLead.CONTATADO);
        assertThat(resposta.observacao()).isEqualTo("Ligou");
        assertThat(lead.getDataAtualizacao()).isEqualTo(LocalDateTime.now(Fixtures.CLOCK));
    }

    @Test
    @DisplayName("transição inválida gera RegraNegocioException (422)")
    void alterarStatusInvalido() {
        Lead lead = Fixtures.lead(1, cliente, veiculo);
        lead.setStatus(StatusLead.CONVERTIDO);
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(escopo.podeAcessar(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.alterarStatus(1L, new LeadStatusRequest(StatusLead.ABERTO, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("CONVERTIDO para ABERTO");
        assertThat(lead.getStatus()).isEqualTo(StatusLead.CONVERTIDO);
    }

    @Test
    @DisplayName("lead de outra concessionária é tratado como inexistente (404)")
    void buscarForaDoEscopo() {
        when(leadRepository.findById(1L)).thenReturn(Optional.of(Fixtures.lead(1, cliente, veiculo)));
        when(escopo.podeAcessar(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.buscar(1L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarInexistente() {
        when(leadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("não cria lead manual se o veículo já tem lead ativo (409)")
    void criarComLeadAtivo() {
        when(clienteService.buscarReferencia(10L)).thenReturn(cliente);
        when(veiculoRepository.findById(100L)).thenReturn(Optional.of(veiculo));
        when(leadRepository.existsByVeiculoIdAndStatusIn(100L, StatusLead.ativos())).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new LeadRequest(10L, 100L, "motivo", Prioridade.MEDIA)))
                .isInstanceOf(ConflitoException.class);
        verify(leadRepository, never()).save(any());
    }

    @Test
    @DisplayName("não cria lead se o veículo não pertence ao cliente (422)")
    void criarVeiculoDeOutroCliente() {
        Cliente outro = Fixtures.cliente(11, concessionaria, null, null);
        when(clienteService.buscarReferencia(11L)).thenReturn(outro);
        when(veiculoRepository.findById(100L)).thenReturn(Optional.of(veiculo));

        assertThatThrownBy(() -> service.criar(new LeadRequest(11L, 100L, "motivo", Prioridade.MEDIA)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não pertence");
    }

    @Test
    @DisplayName("cria lead manual na concessionária preferida do cliente")
    void criarManual() {
        when(clienteService.buscarReferencia(10L)).thenReturn(cliente);
        when(veiculoRepository.findById(100L)).thenReturn(Optional.of(veiculo));
        when(leadRepository.existsByVeiculoIdAndStatusIn(100L, StatusLead.ativos())).thenReturn(false);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        LeadResponse resposta = service.criar(new LeadRequest(10L, 100L, "  Revisão vencida  ", Prioridade.MEDIA));

        assertThat(resposta.origem()).isEqualTo(OrigemLead.MANUAL);
        assertThat(resposta.concessionariaId()).isEqualTo(1L);
        assertThat(resposta.motivo()).isEqualTo("Revisão vencida");
    }

    @Test
    @DisplayName("serviço pago converte os leads ativos do veículo")
    void converterLeadsAtivos() {
        Lead lead = Fixtures.lead(1, cliente, veiculo);
        when(leadRepository.findByVeiculoIdAndStatusIn(eq(100L), any())).thenReturn(List.of(lead));

        int convertidos = service.converterLeadsAtivos(100L, 77L);

        assertThat(convertidos).isEqualTo(1);
        assertThat(lead.getStatus()).isEqualTo(StatusLead.CONVERTIDO);
        assertThat(lead.getObservacao()).contains("#77");
    }
}
