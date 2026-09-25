package br.com.fiap.fordretention.model.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida do lead. Transições permitidas:
 * ABERTO → CONTATADO | AGENDADO | PERDIDO
 * CONTATADO → AGENDADO | PERDIDO
 * AGENDADO → CONVERTIDO | CONTATADO (reagendamento) | PERDIDO
 * CONVERTIDO e PERDIDO são finais.
 */
public enum StatusLead {
    ABERTO,
    CONTATADO,
    AGENDADO,
    CONVERTIDO,
    PERDIDO;

    public Set<StatusLead> proximosPermitidos() {
        return switch (this) {
            case ABERTO -> EnumSet.of(CONTATADO, AGENDADO, PERDIDO);
            case CONTATADO -> EnumSet.of(AGENDADO, PERDIDO);
            case AGENDADO -> EnumSet.of(CONVERTIDO, CONTATADO, PERDIDO);
            case CONVERTIDO, PERDIDO -> EnumSet.noneOf(StatusLead.class);
        };
    }

    public boolean podeIrPara(StatusLead destino) {
        return proximosPermitidos().contains(destino);
    }

    public boolean ativo() {
        return this != CONVERTIDO && this != PERDIDO;
    }

    public static Set<StatusLead> ativos() {
        return EnumSet.of(ABERTO, CONTATADO, AGENDADO);
    }
}
