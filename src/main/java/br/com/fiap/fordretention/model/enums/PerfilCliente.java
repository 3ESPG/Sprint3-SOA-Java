package br.com.fiap.fordretention.model.enums;

/** Perfis de retenção produzidos pelo modelo de ML (Random Forest). */
public enum PerfilCliente {
    FIEL("Cliente Fiel", "Realiza serviços com regularidade na rede Ford"),
    ABANDONO("Cliente de Abandono", "Alto risco de deixar a rede oficial Ford"),
    ESQUECIDO("Cliente Esquecido", "Sem serviço recente; precisa ser lembrado da manutenção"),
    ECONOMICO("Cliente Econômico", "Sensível a preço; responde a ofertas e pacotes");

    private final String nome;
    private final String descricao;

    PerfilCliente(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Perfis que disparam a geração automática de leads quando o score passa do limite. */
    public boolean geraLeadAutomatico() {
        return this == ABANDONO || this == ESQUECIDO;
    }
}
