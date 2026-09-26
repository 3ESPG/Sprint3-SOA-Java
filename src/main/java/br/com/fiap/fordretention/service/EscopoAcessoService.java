package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.security.UsuarioAutenticado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Aplica o escopo de dados a partir do concessionariaId presente no JWT:
 * ADMIN enxerga tudo; GESTOR_CONCESSIONARIA e CONSULTOR enxergam apenas a própria concessionária.
 */
@Service
public class EscopoAcessoService {

    private static final Logger log = LoggerFactory.getLogger(EscopoAcessoService.class);

    public UsuarioAutenticado usuarioAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new AuthenticationCredentialsNotFoundException("Usuário não autenticado");
        }
        return usuario;
    }

    public boolean isAdmin() {
        return usuarioAtual().isAdmin();
    }

    /** Concessionária à qual o usuário está restrito; nulo para ADMIN. */
    public Long concessionariaRestrita() {
        UsuarioAutenticado usuario = usuarioAtual();
        return usuario.isAdmin() ? null : usuario.concessionariaId();
    }

    /**
     * Resolve o filtro de concessionária de uma listagem: o ADMIN pode escolher qualquer uma (ou nenhuma);
     * os demais perfis são sempre forçados para a concessionária do token, ignorando o parâmetro.
     */
    public Long resolverFiltro(Long concessionariaSolicitada) {
        UsuarioAutenticado usuario = usuarioAtual();
        return usuario.isAdmin() ? concessionariaSolicitada : usuario.concessionariaId();
    }

    /**
     * Leitura: recursos fora do escopo devem ser tratados como inexistentes (404) pelo chamador.
     * A tentativa é registrada como evento de segurança: o 404 esconde o recurso do usuário, mas não
     * da equipe de monitoramento (é o sinal de enumeração/BOLA usado na resposta a incidentes).
     */
    public boolean podeAcessar(Long concessionariaId) {
        boolean permitido = permitido(concessionariaId);
        if (!permitido) {
            log.atWarn()
                    .addKeyValue("evento", "authz.fora_do_escopo")
                    .addKeyValue("concessionariaAlvo", concessionariaId)
                    .log("Acesso a recurso de outra concessionária (respondido como 404)");
        }
        return permitido;
    }

    /** Escrita: o payload aponta para outra concessionária → 403 (logado como authz.acesso_negado). */
    public void exigirAcessoParaEscrita(Long concessionariaId) {
        if (!permitido(concessionariaId)) {
            throw new AccessDeniedException("Operação permitida apenas para dados da sua concessionária");
        }
    }

    private boolean permitido(Long concessionariaId) {
        UsuarioAutenticado usuario = usuarioAtual();
        return usuario.isAdmin() || Objects.equals(usuario.concessionariaId(), concessionariaId);
    }
}
