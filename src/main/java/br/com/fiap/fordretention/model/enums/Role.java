package br.com.fiap.fordretention.model.enums;

public enum Role {
    /** Equipe Ford: acesso total. */
    ADMIN,
    /** Gestor de concessionária: acessa apenas os dados da própria concessionária. */
    GESTOR_CONCESSIONARIA,
    /** Consultor de serviços: visualiza dados e atualiza leads da própria concessionária. */
    CONSULTOR;

    public String authority() {
        return "ROLE_" + name();
    }
}
