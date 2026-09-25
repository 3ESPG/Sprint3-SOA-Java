package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.model.Cliente;
import br.com.fiap.fordretention.model.Concessionaria;
import br.com.fiap.fordretention.model.Lead;
import br.com.fiap.fordretention.model.Veiculo;
import br.com.fiap.fordretention.model.enums.OrigemLead;
import br.com.fiap.fordretention.model.enums.PerfilCliente;
import br.com.fiap.fordretention.model.enums.Prioridade;
import br.com.fiap.fordretention.model.enums.StatusGarantia;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Objetos de domínio para os testes unitários dos services. */
final class Fixtures {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T12:00:00Z"), ZoneOffset.UTC);

    private Fixtures() {
    }

    static Concessionaria concessionaria(long id) {
        Concessionaria c = new Concessionaria("Ford " + id, "São Paulo", "SP", "1122233300018" + id);
        c.setId(id);
        return c;
    }

    static Cliente cliente(long id, Concessionaria concessionaria, PerfilCliente perfil, String score) {
        Cliente c = new Cliente("Cliente " + id, "cliente" + id + "@email.com", "+5511999990000", concessionaria);
        c.setId(id);
        c.setPerfil(perfil);
        c.setScoreRisco(score == null ? null : new BigDecimal(score));
        return c;
    }

    static Veiculo veiculo(long id, Cliente cliente, String modelo, int ano) {
        Veiculo v = new Veiculo("9BFZZZ55LA00000%02d".formatted(id), modelo, ano, 50_000, cliente,
                StatusGarantia.EXPIRADA);
        v.setId(id);
        return v;
    }

    static Lead lead(long id, Cliente cliente, Veiculo veiculo) {
        Lead l = new Lead(cliente, veiculo, cliente.getConcessionariaPreferida(), "motivo", Prioridade.ALTA,
                OrigemLead.MODELO_ML, LocalDateTime.now(CLOCK).minusDays(3));
        l.setId(id);
        return l;
    }
}
