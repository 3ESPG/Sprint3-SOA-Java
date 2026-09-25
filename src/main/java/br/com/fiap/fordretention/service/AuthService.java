package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.auth.LoginRequest;
import br.com.fiap.fordretention.dto.auth.RegisterRequest;
import br.com.fiap.fordretention.dto.auth.TokenResponse;
import br.com.fiap.fordretention.dto.usuario.UsuarioResponse;
import br.com.fiap.fordretention.mapper.UsuarioMapper;
import br.com.fiap.fordretention.security.JwtService;
import br.com.fiap.fordretention.security.UsuarioAutenticado;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

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
        Authentication autenticacao = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(Locale.ROOT),
                        request.senha()));
        UsuarioAutenticado usuario = (UsuarioAutenticado) autenticacao.getPrincipal();
        String token = jwtService.gerarToken(usuario);
        return TokenResponse.bearer(token, jwtService.expiracaoEmSegundos(), usuarioMapper.toResponse(usuario));
    }

    public UsuarioResponse registrar(RegisterRequest request) {
        return usuarioService.registrar(request);
    }
}
