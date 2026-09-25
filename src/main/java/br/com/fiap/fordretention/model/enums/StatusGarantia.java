package br.com.fiap.fordretention.model.enums;

public enum StatusGarantia {
    ATIVA,
    EXPIRADA,
    ESTENDIDA;

    public boolean coberto() {
        return this != EXPIRADA;
    }
}
