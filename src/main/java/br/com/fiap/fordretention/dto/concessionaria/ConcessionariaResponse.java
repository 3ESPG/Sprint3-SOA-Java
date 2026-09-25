package br.com.fiap.fordretention.dto.concessionaria;

public record ConcessionariaResponse(
        Long id,
        String nome,
        String cidade,
        String estado,
        String cnpj
) {
}
