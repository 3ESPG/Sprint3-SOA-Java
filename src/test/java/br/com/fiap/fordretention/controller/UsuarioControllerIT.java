package br.com.fiap.fordretention.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioControllerIT extends IntegrationTestSupport {

    @Test
    @DisplayName("gestor aprova consultor pendente da própria concessionária, que então consegue logar")
    void aprovarConsultor() throws Exception {
        mvc.perform(patch("/usuarios/{id}/ativacao", pendente.getId()).with(tokenDe(gestorSp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ativo": true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "pendente@ford.com", "senha": "%s"}""".formatted(SENHA)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("gestor do RJ não enxerga usuário de SP → 404")
    void gestorOutraConcessionaria() throws Exception {
        mvc.perform(patch("/usuarios/{id}/ativacao", pendente.getId()).with(tokenDe(gestorRj))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ativo": true}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("somente ADMIN cria usuários com qualquer perfil")
    void criarUsuario() throws Exception {
        String corpo = """
                {"nome": "Gestor MG", "email": "gestor.mg@ford.com", "senha": "Senha1234",
                 "role": "GESTOR_CONCESSIONARIA", "concessionariaId": %d}""".formatted(concSp.getId());

        mvc.perform(post("/usuarios").with(tokenDe(gestorSp)).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/usuarios").with(tokenDe(admin)).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("consultor só consegue ver o próprio usuário")
    void consultorVeSoASiMesmo() throws Exception {
        mvc.perform(get("/usuarios/{id}", consultorSp.getId()).with(tokenDe(consultorSp)))
                .andExpect(status().isOk());
        mvc.perform(get("/usuarios/{id}", gestorSp.getId()).with(tokenDe(consultorSp)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/usuarios").with(tokenDe(consultorSp)))
                .andExpect(status().isForbidden());
    }
}
