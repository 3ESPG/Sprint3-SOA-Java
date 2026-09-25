package br.com.fiap.fordretention.security;

import br.com.fiap.fordretention.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-unitario-com-mais-de-32-caracteres";
    private static final Instant AGORA = Instant.parse("2026-09-24T12:00:00Z");
    private static final JwtProperties PROPS = new JwtProperties(SEGREDO, Duration.ofHours(1), "ford-retention-ai");

    private final JwtService jwtService = new JwtService(PROPS, Clock.fixed(AGORA, ZoneOffset.UTC));

    private static UsuarioAutenticado gestor() {
        return new UsuarioAutenticado(3L, "Paula", "gestor@ford.com", "hash", Role.GESTOR_CONCESSIONARIA, 1L, true);
    }

    @Test
    @DisplayName("gera token com sub, role, concessionariaId, iat e exp e o valida de volta")
    void geraEValidaToken() {
        String token = jwtService.gerarToken(gestor());

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8)))
                .clock(() -> java.util.Date.from(AGORA))
                .build().parseSignedClaims(token).getPayload();
        assertThat(claims.getSubject()).isEqualTo("gestor@ford.com");
        assertThat(claims.get("role", String.class)).isEqualTo("GESTOR_CONCESSIONARIA");
        assertThat(claims.get("concessionariaId", Number.class).longValue()).isEqualTo(1L);
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(AGORA);
        assertThat(claims.getExpiration().toInstant()).isEqualTo(AGORA.plus(Duration.ofHours(1)));

        UsuarioAutenticado usuario = jwtService.validarToken(token);
        assertThat(usuario.id()).isEqualTo(3L);
        assertThat(usuario.email()).isEqualTo("gestor@ford.com");
        assertThat(usuario.role()).isEqualTo(Role.GESTOR_CONCESSIONARIA);
        assertThat(usuario.concessionariaId()).isEqualTo(1L);
        assertThat(usuario.getPassword()).isNull();
        assertThat(usuario.getAuthorities()).extracting("authority").containsExactly("ROLE_GESTOR_CONCESSIONARIA");
    }

    @Test
    @DisplayName("token de ADMIN não carrega concessionariaId")
    void adminSemConcessionaria() {
        var admin = new UsuarioAutenticado(1L, "Admin", "admin@ford.com", "hash", Role.ADMIN, null, true);

        UsuarioAutenticado validado = jwtService.validarToken(jwtService.gerarToken(admin));

        assertThat(validado.concessionariaId()).isNull();
        assertThat(validado.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("rejeita token expirado")
    void rejeitaTokenExpirado() {
        String token = jwtService.gerarToken(gestor());
        JwtService duasHorasDepois = new JwtService(PROPS, Clock.fixed(AGORA.plus(Duration.ofHours(2)), ZoneOffset.UTC));

        assertThatThrownBy(() -> duasHorasDepois.validarToken(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("rejeita token assinado com outro segredo")
    void rejeitaAssinaturaInvalida() {
        JwtService outroSegredo = new JwtService(
                new JwtProperties("outro-segredo-qualquer-com-mais-de-32-caracteres", Duration.ofHours(1),
                        "ford-retention-ai"),
                Clock.fixed(AGORA, ZoneOffset.UTC));
        String token = outroSegredo.gerarToken(gestor());

        assertThatThrownBy(() -> jwtService.validarToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("rejeita token com payload adulterado (ex.: troca de role)")
    void rejeitaTokenAdulterado() {
        String[] partes = jwtService.gerarToken(gestor()).split("\\.");
        String payloadAdulterado = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"gestor@ford.com\",\"iss\":\"ford-retention-ai\",\"role\":\"ADMIN\",\"exp\":"
                        + AGORA.plusSeconds(3600).getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> jwtService.validarToken(partes[0] + "." + payloadAdulterado + "." + partes[2]))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("rejeita token de outro emissor")
    void rejeitaEmissorDiferente() {
        JwtService outroEmissor = new JwtService(new JwtProperties(SEGREDO, Duration.ofHours(1), "outro-sistema"),
                Clock.fixed(AGORA, ZoneOffset.UTC));

        assertThatThrownBy(() -> jwtService.validarToken(outroEmissor.gerarToken(gestor())))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("não aceita segredo com menos de 256 bits")
    void recusaSegredoFraco() {
        var fraco = new JwtProperties("curto", Duration.ofHours(1), "ford-retention-ai");

        assertThatThrownBy(() -> new JwtService(fraco, Clock.systemUTC())).isInstanceOf(WeakKeyException.class);
    }

    @Test
    void expiracaoEmSegundos() {
        assertThat(jwtService.expiracaoEmSegundos()).isEqualTo(3600);
    }
}
