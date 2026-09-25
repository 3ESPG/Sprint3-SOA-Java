package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.auth.LoginRequest;
import br.com.fiap.fordretention.dto.auth.RegisterRequest;
import br.com.fiap.fordretention.dto.auth.TokenResponse;
import br.com.fiap.fordretention.dto.usuario.UsuarioResponse;
import br.com.fiap.fordretention.mapper.UsuarioMapper;
import br.com.fiap.fordretention.security.JwtService;
import br.com.fiap.fordretention.security.LogSeguranca;
import br.com.fiap.fordretention.security.UsuarioAutenticado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                       UsuarioService usuarioService, UsuarioMapper usuarioMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
        this.usuarioMapper = usuarioMapper;
    }

    /**
     * Autentica com BCrypt via AuthenticationManager e emite o JWT.
     * Lança BadCredentialsException/DisabledException, convertidas em 401 pelo GlobalExceptionHandler.
     */
    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Authentication autenticacao;
        try {
            autenticacao = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.senha()));
        } catch (AuthenticationException e) {
            log.atWarn()
                    .addKeyValue("evento", "auth.login_falha")
                    .addKeyValue("email", LogSeguranca.mascararEmail(email))
                    .addKeyValue("motivo", e.getClass().getSimpleName())
                    .log("Falha de login");
            throw e;
        }
        UsuarioAutenticado usuario = (UsuarioAutenticado) autenticacao.getPrincipal();
        String token = jwtService.gerarToken(usuario);
        // no MDC (e não como par chave-valor) para não duplicar a chave se já houver um Bearer na requisição
        MDC.put("usuarioId", String.valueOf(usuario.id()));
        MDC.put("role", usuario.role().name());
        log.atInfo()
                .addKeyValue("evento", "auth.login_sucesso")
                .addKeyValue("concessionariaId", usuario.concessionariaId())
                .log("Login realizado");
        return TokenResponse.bearer(token, jwtService.expiracaoEmSegundos(), usuarioMapper.toResponse(usuario));
    }

    public UsuarioResponse registrar(RegisterRequest request) {
        return usuarioService.registrar(request);
    }
}
