package br.com.fiap.fordretention.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Telefone do cliente (dado pessoal) cifrado em repouso e transparente para a API. */
class CriptografiaDadosPessoaisIT extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("telefone é gravado cifrado (AES-GCM) e a API devolve o valor em claro")
    void telefoneCifradoNoBanco() throws Exception {
        String corpo = """
                {"nome": "Carla Lima", "email": "carla@email.com", "telefone": "+5511988887777",
                 "concessionariaPreferidaId": %d}""".formatted(concSp.getId());
        mvc.perform(post("/clientes").with(tokenDe(gestorSp)).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();

        String noBanco = jdbc.queryForObject(
                "SELECT telefone FROM clientes WHERE email = 'carla@email.com'", String.class);
        assertThat(noBanco).startsWith("v1:").doesNotContain("988887777");

        Long id = jdbc.queryForObject("SELECT id FROM clientes WHERE email = 'carla@email.com'", Long.class);
        mvc.perform(get("/clientes/{id}", id).with(tokenDe(gestorSp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefone").value("+5511988887777"));
    }
}
