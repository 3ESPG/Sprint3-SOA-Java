package br.com.fiap.fordretention.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VeiculoControllerIT extends IntegrationTestSupport {

    @Test
    @DisplayName("GET /veiculos?modelo=ranger&idadeMin=4 filtra veículos antigos")
    void filtroModeloEIdade() throws Exception {
        mvc.perform(get("/veiculos").param("modelo", "ranger").param("idadeMin", "4").with(tokenDe(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].vin").value("9BFZZZ55LA0000001"))
                .andExpect(jsonPath("$.content[0].idade").value(7));
    }

    @Test
    @DisplayName("gestor do RJ só enxerga o parque do RJ")
    void escopo() throws Exception {
        mvc.perform(get("/veiculos").with(tokenDe(gestorRj)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].modelo").value("Ka"));
        mvc.perform(get("/veiculos/{id}", rangerAntigaMaria.getId()).with(tokenDe(gestorRj)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("idadeMin negativo ou ordenação inválida → 400")
    void parametrosInvalidos() throws Exception {
        mvc.perform(get("/veiculos").param("idadeMin", "-1").with(tokenDe(admin)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/veiculos").param("sort", "inexistente").with(tokenDe(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cria → 201; VIN duplicado → 409; VIN inválido → 400")
    void criar() throws Exception {
        String corpo = """
                {"vin": "%s", "modelo": "Maverick", "ano": %d, "quilometragem": 1000,
                 "clienteId": %d, "statusGarantia": "ATIVA"}""";

        mvc.perform(post("/veiculos").with(tokenDe(gestorSp)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo.formatted("9bfzzz55la0000099", ANO_ATUAL, joaoSp.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.vin").value("9BFZZZ55LA0000099"));

        mvc.perform(post("/veiculos").with(tokenDe(gestorSp)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo.formatted("9BFZZZ55LA0000001", ANO_ATUAL, joaoSp.getId())))
                .andExpect(status().isConflict());

        mvc.perform(post("/veiculos").with(tokenDe(gestorSp)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo.formatted("VIN-COM-O-INVALIDO", ANO_ATUAL, joaoSp.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("vin"));
    }

    @Test
    @DisplayName("PATCH com quilometragem menor que a atual → 422")
    void quilometragemMenor() throws Exception {
        mvc.perform(patch("/veiculos/{id}", rangerAntigaMaria.getId()).with(tokenDe(gestorSp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quilometragem": 1000}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("CONSULTOR não exclui veículo → 403")
    void consultorNaoExclui() throws Exception {
        mvc.perform(delete("/veiculos/{id}", territoryNovaJoao.getId()).with(tokenDe(consultorSp)))
                .andExpect(status().isForbidden());
    }
}
