package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.security.JwtProperties;
import br.com.fiap.fordretention.security.JwtService;
import br.com.fiap.fordretention.security.UsuarioAutenticado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regras transversais de autenticação (401) e autorização por perfil (403). */
class SegurancaIT extends IntegrationTestSupport {

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    @DisplayName("sem token → 401 com header WWW-Authenticate")
    void semToken() throws Exception {
        mvc.perform(get("/leads"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/leads"));
    }

    @Test
    @DisplayName("token malformado → 401 'Token inválido'")
    void tokenInvalido() throws Exception {
        mvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token inválido"));
    }

    @Test
    @DisplayName("token expirado → 401 'Token expirado'")
    void tokenExpirado() throws Exception {
        JwtService emitidoOntem = new JwtService(jwtProperties,
                Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC));
        String token = emitidoOntem.gerarToken(UsuarioAutenticado.of(admin));

        mvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token expirado"));
    }

    @Test
    @DisplayName("Swagger e health check são públicos")
    void endpointsPublicos() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CONSULTOR")
    @DisplayName("CONSULTOR não pode excluir lead (regra de URL no SecurityFilterChain) → 403")
    void consultorNaoExcluiLead() throws Exception {
        mvc.perform(delete("/leads/{id}", leadMaria.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "GESTOR_CONCESSIONARIA")
    @DisplayName("GESTOR não pode gravar perfil do ML → 403")
    void gestorNaoGravaPerfil() throws Exception {
        mvc.perform(put("/clientes/{id}/perfil", mariaSp.getId())
                        .contentType("application/json")
                        .content("""
                                {"perfil": "FIEL", "scoreRisco": 0.1}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CONSULTOR")
    @DisplayName("CONSULTOR não pode cadastrar cliente (@PreAuthorize) → 403")
    void consultorNaoCriaCliente() throws Exception {
        mvc.perform(post("/clientes")
                        .contentType("application/json")
                        .content("""
                                {"nome": "X", "email": "x@email.com", "concessionariaPreferidaId": 1}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Seu perfil não tem permissão para esta operação"));
    }

    @Test
    @DisplayName("CONSULTOR não acessa o Service Share → 403")
    void consultorNaoVeServiceShare() throws Exception {
        mvc.perform(get("/concessionarias/{id}/service-share", concSp.getId()).with(tokenDe(consultorSp)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("rota inexistente autenticada → 404 no formato padrão")
    void rotaInexistente() throws Exception {
        mvc.perform(get("/nao-existe").with(tokenDe(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("corpo acima de 64 KB → 413 antes de desserializar")
    void corpoGrandeDemais() throws Exception {
        String enorme = "{\"email\": \"" + "a".repeat(70 * 1024) + "@x.com\", \"senha\": \"x\"}";
        mvc.perform(post("/auth/login").contentType("application/json").content(enorme))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    @Test
    @DisplayName("login com senha acima de 72 caracteres → 400 (limite do BCrypt)")
    void senhaLongaDemais() throws Exception {
        String corpo = "{\"email\": \"admin@ford.com\", \"senha\": \"" + "x".repeat(73) + "\"}";
        mvc.perform(post("/auth/login").contentType("application/json").content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("senha"));
    }
}
