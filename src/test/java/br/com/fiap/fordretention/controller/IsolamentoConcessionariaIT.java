package br.com.fiap.fordretention.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controle de acesso horizontal (OWASP API1:2023 – BOLA): GESTOR e CONSULTOR só enxergam dados da
 * concessionária do próprio token. Recurso de outra concessionária responde 404 (e não 403) para não
 * confirmar que o id existe.
 */
class IsolamentoConcessionariaIT extends IntegrationTestSupport {

    @Test
    @DisplayName("consultor de SP não acessa lead do RJ → 404")
    void consultorNaoAcessaLeadDeOutraConcessionaria() throws Exception {
        mvc.perform(get("/leads/{id}", leadFernanda.getId()).with(tokenDe(consultorSp)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("gestor de SP acessa lead da própria concessionária → 200")
    void gestorAcessaLeadDaPropriaConcessionaria() throws Exception {
        mvc.perform(get("/leads/{id}", leadMaria.getId()).with(tokenDe(gestorSp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leadMaria.getId()));
    }

    @Test
    @DisplayName("gestor do RJ não acessa cliente de SP → 404")
    void gestorNaoAcessaClienteDeOutraConcessionaria() throws Exception {
        mvc.perform(get("/clientes/{id}", mariaSp.getId()).with(tokenDe(gestorRj)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("listagem do consultor ignora ?concessionariaId de outra concessionária")
    void listagemForcadaParaConcessionariaDoToken() throws Exception {
        mvc.perform(get("/leads").param("concessionariaId", concRj.getId().toString()).with(tokenDe(consultorSp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].concessionariaId", everyItem(is(concSp.getId().intValue()))));
    }

    @Test
    @DisplayName("ADMIN (Ford) acessa leads de qualquer concessionária → 200")
    void adminAcessaQualquerConcessionaria() throws Exception {
        mvc.perform(get("/leads/{id}", leadFernanda.getId()).with(tokenDe(admin)))
                .andExpect(status().isOk());
    }
}
