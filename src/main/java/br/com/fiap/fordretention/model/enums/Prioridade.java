package br.com.fiap.fordretention.model.enums;

import java.math.BigDecimal;

public enum Prioridade {
    ALTA,
    MEDIA,
    BAIXA;

    private static final BigDecimal LIMITE_ALTA = new BigDecimal("0.90");
    private static final BigDecimal LIMITE_MEDIA = new BigDecimal("0.80");

    public static Prioridade porScore(BigDecimal score) {
        if (score.compareTo(LIMITE_ALTA) >= 0) {
            return ALTA;
        }
        if (score.compareTo(LIMITE_MEDIA) >= 0) {
            return MEDIA;
        }
        return BAIXA;
    }
}
