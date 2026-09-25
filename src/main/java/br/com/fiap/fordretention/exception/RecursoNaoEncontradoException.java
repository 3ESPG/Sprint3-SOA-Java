package br.com.fiap.fordretention.exception;

/** 404 – recurso inexistente ou fora do escopo da concessionária do usuário. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super("%s %s não encontrado(a)".formatted(recurso, id));
    }

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
