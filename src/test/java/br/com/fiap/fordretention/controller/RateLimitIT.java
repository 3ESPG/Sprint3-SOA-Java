package br.com.fiap.fordretention.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rate limiting (Bucket4j): 429 com Retry-After no formato padrão de erro. */
@TestPropertySource(properties = {
        "app.rate-limit.login-por-minuto=5",
        "app.rate-limit.autenticado-por-minuto=3"
})
class RateLimitIT extends IntegrationTestSupport {

    @Test
    @DisplayName("6ª tentativa de login no mesmo minuto → 429 com Retry-After")
    void forcaBrutaNoLogin() throws Exception {
        String senhaErrada = """
                {"email": "admin@ford.com", "senha": "chute-errado"}""";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(senhaErrada))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(senhaErrada))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    @DisplayName("usuário autenticado acima do limite → 429 (chave por usuário do token)")
    void limitePorUsuario() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/leads").with(tokenDe(gestorSp)))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }

        mvc.perform(get("/leads").with(tokenDe(gestorSp)))
                .andExpect(status().isTooManyRequests());
        // outro usuário tem o próprio bucket
        mvc.perform(get("/leads").with(tokenDe(gestorRj)))
                .andExpect(status().isOk());
    }
}
