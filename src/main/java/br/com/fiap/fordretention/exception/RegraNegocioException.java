package br.com.fiap.fordretention.exception;

/** 422 – payload sintaticamente válido, mas que viola uma regra de negócio. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
