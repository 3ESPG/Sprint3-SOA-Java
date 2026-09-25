package br.com.fiap.fordretention.exception;

/** 409 – violação de unicidade ou operação bloqueada por vínculos existentes. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
