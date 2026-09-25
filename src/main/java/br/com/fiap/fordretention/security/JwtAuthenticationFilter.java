package br.com.fiap.fordretention.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lê o header "Authorization: Bearer &lt;jwt&gt;", valida assinatura e expiração e popula o
 * SecurityContext. Se o token for inválido a requisição segue sem autenticação e o
 * {@link RestAuthenticationEntryPoint} responde 401 (endpoints públicos continuam acessíveis).
 * <p>
 * Não é um @Component de propósito: assim ele só roda dentro da SecurityFilterChain,
 * e não duas vezes (também como filtro do servlet).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String ATRIBUTO_ERRO_JWT = "jwt.erro";
    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIXO)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIXO.length()).trim();
        try {
            UsuarioAutenticado usuario = jwtService.validarToken(token);
            var autenticacao = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacao);
            SecurityContextHolder.setContext(contexto);
            MDC.put("usuarioId", String.valueOf(usuario.id()));
            MDC.put("role", usuario.role().name());
        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            request.setAttribute(ATRIBUTO_ERRO_JWT, "Token expirado");
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            request.setAttribute(ATRIBUTO_ERRO_JWT, "Token inválido");
        }

        chain.doFilter(request, response);
    }
}
