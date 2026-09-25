package br.com.fiap.fordretention.model;

import br.com.fiap.fordretention.model.enums.PerfilCliente;
import br.com.fiap.fordretention.model.enums.Prioridade;
import br.com.fiap.fordretention.model.enums.StatusLead;
import br.com.fiap.fordretention.model.enums.TipoServico;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasDominioTest {

    @ParameterizedTest(name = "{0} → {1} permitido? {2}")
    @CsvSource({
            "ABERTO, CONTATADO, true",
            "ABERTO, AGENDADO, true",
            "ABERTO, CONVERTIDO, false",
            "CONTATADO, AGENDADO, true",
            "CONTATADO, ABERTO, false",
            "AGENDADO, CONVERTIDO, true",
            "AGENDADO, CONTATADO, true",
            "CONVERTIDO, ABERTO, false",
            "PERDIDO, ABERTO, false",
            "ABERTO, ABERTO, false"
    })
    void maquinaDeEstadosDoLead(StatusLead origem, StatusLead destino, boolean permitido) {
        assertThat(origem.podeIrPara(destino)).isEqualTo(permitido);
    }

    @Test
    void statusFinaisNaoSaoAtivos() {
        assertThat(StatusLead.ativos()).containsExactlyInAnyOrder(StatusLead.ABERTO, StatusLead.CONTATADO,
                StatusLead.AGENDADO);
        assertThat(StatusLead.CONVERTIDO.ativo()).isFalse();
        assertThat(StatusLead.PERDIDO.ativo()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"0.95, ALTA", "0.90, ALTA", "0.85, MEDIA", "0.80, MEDIA", "0.79, BAIXA", "0.70, BAIXA"})
    void prioridadePorScore(BigDecimal score, Prioridade esperada) {
        assertThat(Prioridade.porScore(score)).isEqualTo(esperada);
    }

    @Test
    void apenasAbandonoEEsquecidoGeramLeadAutomatico() {
        assertThat(PerfilCliente.ABANDONO.geraLeadAutomatico()).isTrue();
        assertThat(PerfilCliente.ESQUECIDO.geraLeadAutomatico()).isTrue();
        assertThat(PerfilCliente.FIEL.geraLeadAutomatico()).isFalse();
        assertThat(PerfilCliente.ECONOMICO.geraLeadAutomatico()).isFalse();
    }

    @Test
    void recallEGarantiaNaoPodemSerPagos() {
        assertThat(TipoServico.RECALL.podeSerPago()).isFalse();
        assertThat(TipoServico.GARANTIA.podeSerPago()).isFalse();
        assertThat(TipoServico.REVISAO.podeSerPago()).isTrue();
    }

    @Test
    void idadeDoVeiculo() {
        Veiculo veiculo = new Veiculo();
        veiculo.setAno(2019);
        assertThat(veiculo.idade(2026)).isEqualTo(7);
        veiculo.setAno(2027);
        assertThat(veiculo.idade(2026)).isZero();
    }
}
