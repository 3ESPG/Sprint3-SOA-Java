package br.com.fiap.fordretention.controller;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Monta o header Location das respostas 201 Created. */
final class Localizacao {

    private Localizacao() {
    }

    /** URI do recurso recém-criado a partir da URI da coleção (ex.: POST /clientes → /clientes/42). */
    static URI doNovoRecurso(Object id) {
        return ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(id).toUri();
    }

    /** URI absoluta a partir da raiz da aplicação (ex.: /usuarios/7). */
    static URI de(String caminho, Object id) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(caminho).path("/{id}")
                .buildAndExpand(id).toUri();
    }
}
