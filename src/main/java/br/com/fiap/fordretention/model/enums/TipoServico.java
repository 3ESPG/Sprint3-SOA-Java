package br.com.fiap.fordretention.model.enums;

public enum TipoServico {
    REVISAO,
    REPARO,
    PECAS,
    RECALL,
    GARANTIA;

    /** RECALL e GARANTIA são custeados pela Ford e não podem ser registrados como serviço pago. */
    public boolean podeSerPago() {
        return this != RECALL && this != GARANTIA;
    }
}
