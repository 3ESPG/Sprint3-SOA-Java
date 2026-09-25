package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.model.Concessionaria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConcessionariaControllerIT extends IntegrationTestSupport {

    private static final String NOVA = """
            {"nome": "Ford Minas Motors", "cidade": "Belo Horizonte", "estado": "mg", "cnpj": "33.444.555/0001-81"}""";

    @Test
    @DisplayName("ADMIN cria → 201 + Location, CNPJ normalizado")
    void adminCria() throws Exception {
        mvc.perform(post("/concessionarias").with(tokenDe(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(NOVA))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.matchesPattern(
                        "http://localhost/concessionarias/\\d+")))
                .andExpect(jsonPath("$.cnpj").value("33444555000181"))
                .andExpect(jsonPath("$.estado").value("MG"));
    }

    @Test
    @DisplayName("CNPJ com dígito verificador inválido → 400")
    void cnpjInvalido() throws Exception {
        mvc.perform(post("/concessionarias").with(tokenDe(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "X", "cidade": "Y", "estado": "SPX", "cnpj": "11.111.111/1111-11"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("cnpj")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("estado")));
    }

    @Test
    @DisplayName("CNPJ já cadastrado → 409")
    void cnpjDuplicado() throws Exception {
        mvc.perform(post("/concessionarias").with(tokenDe(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Dup", "cidade": "SP", "estado": "SP", "cnpj": "11222333000181"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GESTOR tentando criar → 403")
    void gestorNaoCria() throws Exception {
        mvc.perform(post("/concessionarias").with(tokenDe(gestorSp))
                        .contentType(MediaType.APPLICATION_JSON).content(NOVA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("id inexistente → 404")
    void naoEncontrada() throws Exception {
        mvc.perform(get("/concessionarias/{id}", 999_999).with(tokenDe(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(endsWith("não encontrado(a)")));
    }

    @Test
    @DisplayName("gestor de SP consultando a concessionária do RJ → 404 (escopo pelo token)")
    void gestorForaDoEscopo() throws Exception {
        mvc.perform(get("/concessionarias/{id}", concRj.getId()).with(tokenDe(gestorSp)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/concessionarias/{id}", concSp.getId()).with(tokenDe(gestorSp)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN lista todas com filtro e paginação")
    void listarComFiltro() throws Exception {
        mvc.perform(get("/concessionarias").param("estado", "rj").param("size", "5").with(tokenDe(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content[0].nome").value("Ford Rio Sul"));
    }

    @Test
    @DisplayName("PUT substitui, PATCH altera parcialmente")
    void putEPatch() throws Exception {
        mvc.perform(put("/concessionarias/{id}", concRj.getId()).with(tokenDe(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ford Rio Norte", "cidade": "Niterói", "estado": "RJ", "cnpj": "22333444000181"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cidade").value("Niterói"));

        mvc.perform(patch("/concessionarias/{id}", concRj.getId()).with(tokenDe(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ford Rio Premium"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ford Rio Premium"))
                .andExpect(jsonPath("$.cidade").value("Niterói"));
    }

    @Test
    @DisplayName("DELETE sem vínculos → 204; com vínculos → 409")
    void excluir() throws Exception {
        Concessionaria vazia = concessionariaRepository.save(
                new Concessionaria("Vazia", "Curitiba", "PR", "44555666000181"));

        mvc.perform(delete("/concessionarias/{id}", vazia.getId()).with(tokenDe(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/concessionarias/{id}", concSp.getId()).with(tokenDe(admin)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /concessionarias/{id}/service-share calcula o indicador com recortes")
    void serviceShare() throws Exception {
        // Base SP: Maria (Ranger 7 anos, sem serviço) e João (Territory 1 ano, revisão paga há 2 meses)
        mvc.perform(get("/concessionarias/{id}/service-share", concSp.getId()).with(tokenDe(gestorSp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientes").value(2))
                .andExpect(jsonPath("$.clientesComServicoPago").value(1))
                .andExpect(jsonPath("$.serviceShare").value(50.0))
                .andExpect(jsonPath("$.porIdadeVeiculo[0].segmento").value("0-3 anos"))
                .andExpect(jsonPath("$.porIdadeVeiculo[0].share").value(100.0))
                .andExpect(jsonPath("$.porIdadeVeiculo[2].segmento").value("7+ anos"))
                .andExpect(jsonPath("$.porIdadeVeiculo[2].share").value(0.0))
                .andExpect(jsonPath("$.porTipoServico[0].tipo").value("REVISAO"));
    }

    @Test
    @DisplayName("service-share: gestor de outra concessionária → 404; parâmetro inválido → 400")
    void serviceShareErros() throws Exception {
        mvc.perform(get("/concessionarias/{id}/service-share", concSp.getId()).with(tokenDe(gestorRj)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/concessionarias/{id}/service-share", concSp.getId()).param("meses", "0")
                        .with(tokenDe(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("meses"));
    }
}
