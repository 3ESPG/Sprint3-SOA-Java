package br.com.fiap.fordretention.security;

import br.com.fiap.fordretention.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * Geração e validação de JWT (HS256).
 * Claims: sub (e-mail), uid, nome, role, concessionariaId (ausente para ADMIN), iss, iat e exp.
 */
@Service
public class JwtService {

    static final String CLAIM_UID = "uid";
    static final String CLAIM_NOME = "nome";
    static final String CLAIM_ROLE = "role";
    static final String CLAIM_CONCESSIONARIA = "concessionariaId";

    private final SecretKey chave;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.chave = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(UsuarioAutenticado usuario) {
        Instant agora = clock.instant();
        return Jwts.builder()
                .subject(usuario.email())
                .issuer(properties.issuer())
                .claim(CLAIM_UID, usuario.id())
                .claim(CLAIM_NOME, usuario.nome())
                .claim(CLAIM_ROLE, usuario.role().name())
                .claim(CLAIM_CONCESSIONARIA, usuario.concessionariaId())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(properties.expiration())))
                .signWith(chave, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Valida assinatura, emissor e expiração e reconstrói o usuário a partir das claims.
     *
     * @throws JwtException             token inválido, adulterado ou expirado
     * @throws IllegalArgumentException token vazio ou claims inconsistentes
     */
    public UsuarioAutenticado validarToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(chave)
                .requireIssuer(properties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
        return new UsuarioAutenticado(
                comoLong(claims.get(CLAIM_UID)),
                claims.get(CLAIM_NOME, String.class),
                claims.getSubject(),
                null,
                role,
                comoLong(claims.get(CLAIM_CONCESSIONARIA)),
                true);
    }

    public long expiracaoEmSegundos() {
        return properties.expiration().toSeconds();
    }

    private static Long comoLong(Object valor) {
        return valor instanceof Number n ? n.longValue() : null;
    }
}
