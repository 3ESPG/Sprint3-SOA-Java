package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.security.UsuarioAutenticado;
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

    /** Leitura: recursos fora do escopo devem ser tratados como inexistentes (404) pelo chamador. */
    public boolean podeAcessar(Long concessionariaId) {
        UsuarioAutenticado usuario = usuarioAtual();
        return usuario.isAdmin() || Objects.equals(usuario.concessionariaId(), concessionariaId);
    }

    /** Escrita: o payload aponta para outra concessionária → 403. */
    public void exigirAcessoParaEscrita(Long concessionariaId) {
        if (!podeAcessar(concessionariaId)) {
            throw new AccessDeniedException("Operação permitida apenas para dados da sua concessionária");
        }
    }
}
