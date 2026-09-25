package br.com.fiap.fordretention.dto.auth;

import br.com.fiap.fordretention.dto.usuario.UsuarioResponse;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UsuarioResponse usuario
) {
    public static TokenResponse bearer(String token, long expiresInSeconds, UsuarioResponse usuario) {
        return new TokenResponse(token, "Bearer", expiresInSeconds, usuario);
    }
}
