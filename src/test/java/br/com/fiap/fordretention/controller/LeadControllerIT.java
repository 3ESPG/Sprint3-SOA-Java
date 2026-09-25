package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.model.enums.StatusLead;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeadControllerIT extends IntegrationTestSupport {

    @Test
    @DisplayName("GET /leads?status=ABERTO&perfil=ABANDONO (consultor vê só a própria concessionária)")
    void listarComFiltros() throws Exception {
        mvc.perform(get("/leads").param("status", "ABERTO").param("perfil", "ABANDONO").with(tokenDe(consultorSp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].clienteNome").value("Maria Souza"))
                .andExpect(jsonPath("$.content[0].perfilCliente").value("ABANDONO"));

        mvc.perform(get("/leads").param("perfil", "ESQUECIDO").with(tokenDe(consultorSp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("CONSULTOR atualiza o status do lead → 200")
    void consultorAtualizaStatus() throws Exception {
        mvc.perform(patch("/leads/{id}/status", leadMaria.getId()).with(tokenDe(consultorSp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CONTATADO", "observacao": "Cliente pediu retorno"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTATADO"))
                .andExpect(jsonPath("$.observacao").value("Cliente pediu retorno"));
    }

    @Test
    @DisplayName("transição de status inválida → 422")
    void transicaoInvalida() throws Exception {
        mvc.perform(patch("/leads/{id}/status", leadMaria.getId()).with(tokenDe(consultorSp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CONVERTIDO"}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ABERTO para CONVERTIDO")));
    }

    @Test
    @DisplayName("status ausente → 400")
    void statusAusente() throws Exception {
        mvc.perform(patch("/leads/{id}/status", leadMaria.getId()).with(tokenDe(consultorSp))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
    }

    @Test
    @DisplayName("consultor de SP tentando atualizar lead do RJ → 404")
    void leadDeOutraConcessionaria() throws Exception {
        mvc.perform(patch("/leads/{id}/status", leadFernanda.getId()).with(tokenDe(consultorSp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CONTATADO"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CONSULTOR não cria lead (403); GESTOR cria (201); duplicado para o mesmo veículo (409)")
    void criar() throws Exception {
        String corpo = """
                {"clienteId": %d, "veiculoId": %d, "motivo": "Revisão vencida", "prioridade": "MEDIA"}"""
                .formatted(joaoSp.getId(), territoryNovaJoao.getId());

        mvc.perform(post("/leads").with(tokenDe(consultorSp)).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());

        mvc.perform(post("/leads").with(tokenDe(gestorSp)).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.origem").value("MANUAL"))
                .andExpect(jsonPath("$.status").value("ABERTO"));

        mvc.perform(post("/leads").with(tokenDe(gestorSp)).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT em lead finalizado → 422")
    void putLeadFinalizado() throws Exception {
        leadMaria.setStatus(StatusLead.PERDIDO);
        mvc.perform(put("/leads/{id}", leadMaria.getId()).with(tokenDe(gestorSp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId": %d, "veiculoId": %d, "motivo": "Novo", "prioridade": "ALTA"}"""
                                .formatted(mariaSp.getId(), rangerAntigaMaria.getId())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("DELETE: ADMIN → 204; GESTOR → 403; inexistente → 404")
    void excluir() throws Exception {
        mvc.perform(delete("/leads/{id}", leadMaria.getId()).with(tokenDe(gestorSp)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/leads/{id}", leadMaria.getId()).with(tokenDe(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/leads/{id}", leadMaria.getId()).with(tokenDe(admin)))
                .andExpect(status().isNotFound());
    }
}
