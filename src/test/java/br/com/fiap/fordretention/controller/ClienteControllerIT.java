package br.com.fiap.fordretention.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClienteControllerIT extends IntegrationTestSupport {

    @Test
    @DisplayName("gestor lista apenas clientes da própria concessionária, mesmo pedindo outra")
    void listarRespeitaEscopo() throws Exception {
        mvc.perform(get("/clientes").param("concessionariaId", concRj.getId().toString()).with(tokenDe(gestorSp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].concessionariaPreferidaId").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(concSp.getId().intValue()))));
    }

    @Test
    @DisplayName("filtros ?perfil=ABANDONO&scoreMin=0.8")
    void listarComFiltros() throws Exception {
        mvc.perform(get("/clientes").param("perfil", "ABANDONO").param("scoreMin", "0.8").with(tokenDe(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nome").value("Maria Souza"));
    }

    @Test
    @DisplayName("filtro com enum inválido → 400")
    void filtroInvalido() throws Exception {
        mvc.perform(get("/clientes").param("perfil", "VIP").with(tokenDe(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("perfil"));
    }

    @Test
    @DisplayName("gestor cria cliente na própria concessionária → 201 + Location")
    void criar() throws Exception {
        mvc.perform(post("/clientes").with(tokenDe(gestorSp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana Rocha", "email": "Ana@Email.com", "telefone": "+5511988887777",
                                 "concessionariaPreferidaId": %d}""".formatted(concSp.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*/clientes/\\d+$")))
                .andExpect(jsonPath("$.email").value("ana@email.com"));
    }

    @Test
    @DisplayName("gestor de SP criando cliente para o RJ → 403")
    void criarEmOutraConcessionaria() throws Exception {
        mvc.perform(post("/clientes").with(tokenDe(gestorSp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "X", "email": "x@email.com", "concessionariaPreferidaId": %d}"""
                                .formatted(concRj.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("validação → 400; e-mail duplicado → 409")
    void validacoes() throws Exception {
        mvc.perform(post("/clientes").with(tokenDe(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "", "email": "invalido", "telefone": "abc"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(4)));

        mvc.perform(post("/clientes").with(tokenDe(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria 2", "email": "MARIA@email.com", "concessionariaPreferidaId": %d}"""
                                .formatted(concSp.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("cliente de outra concessionária → 404; inexistente → 404")
    void naoEncontrado() throws Exception {
        mvc.perform(get("/clientes/{id}", fernandaRj.getId()).with(tokenDe(consultorSp)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/clientes/{id}", 999_999).with(tokenDe(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT e PATCH")
    void atualizar() throws Exception {
        mvc.perform(put("/clientes/{id}", joaoSp.getId()).with(tokenDe(gestorSp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "João P. Silva", "email": "joao@email.com", "telefone": "+5511900000000",
                                 "concessionariaPreferidaId": %d}""".formatted(concSp.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João P. Silva"));

        mvc.perform(patch("/clientes/{id}", joaoSp.getId()).with(tokenDe(gestorSp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telefone": "+5511911112222"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefone").value("+5511911112222"))
                .andExpect(jsonPath("$.nome").value("João P. Silva"));
    }

    @Test
    @DisplayName("DELETE de cliente com veículos → 409; sem vínculos → 204")
    void excluir() throws Exception {
        mvc.perform(delete("/clientes/{id}", joaoSp.getId()).with(tokenDe(admin)))
                .andExpect(status().isConflict());

        var semVinculo = clienteRepository.save(
                new br.com.fiap.fordretention.model.Cliente("Sem Vínculo", "sv@email.com", null, concSp));
        mvc.perform(delete("/clientes/{id}", semVinculo.getId()).with(tokenDe(admin)))
                .andExpect(status().isNoContent());
        assertThat(clienteRepository.existsById(semVinculo.getId())).isFalse();
    }
}
