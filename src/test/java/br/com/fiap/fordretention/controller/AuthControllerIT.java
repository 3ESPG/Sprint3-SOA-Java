package br.com.fiap.fordretention.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends IntegrationTestSupport {

    @Test
    @DisplayName("POST /auth/login com credenciais válidas → 200 + JWT que acessa endpoint protegido")
    void loginComSucesso() throws Exception {
        String resposta = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "GESTOR.SP@ford.com", "senha": "%s"}""".formatted(SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.accessToken").value(matchesPattern("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$")))
                .andExpect(jsonPath("$.usuario.role").value("GESTOR_CONCESSIONARIA"))
                .andExpect(jsonPath("$.usuario.concessionariaId").value(concSp.getId()))
                .andExpect(jsonPath("$.usuario.senha").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(resposta, "$.accessToken");
        mvc.perform(get("/concessionarias").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(concSp.getId()));
    }

    @Test
    @DisplayName("senha errada → 401 no formato padrão de erro")
    void senhaErrada() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@ford.com", "senha": "errada123"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"))
                .andExpect(jsonPath("$.path").value("/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("usuário inexistente → 401 com a mesma mensagem (não revela quais e-mails existem)")
    void usuarioInexistente() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ninguem@ford.com", "senha": "qualquer1"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    @DisplayName("usuário ainda não aprovado → 401")
    void usuarioPendente() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "pendente@ford.com", "senha": "%s"}""".formatted(SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuário pendente de aprovação"));
    }

    @Test
    @DisplayName("payload inválido → 400 com lista de erros de campo")
    void loginPayloadInvalido() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nao-e-email", "senha": ""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("senha")));
    }

    @Test
    @DisplayName("JSON malformado → 400")
    void jsonMalformado() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{email:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("JSON")));
    }

    @Test
    @DisplayName("POST /auth/register → 201 + Location, consultor inativo")
    void registrar() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Novo Consultor", "email": "novo@ford.com", "senha": "Senha1234",
                                 "concessionariaId": %d}""".formatted(concSp.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*/usuarios/\\d+$")))
                .andExpect(jsonPath("$.role").value("CONSULTOR"))
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    @DisplayName("register não permite escolher role (campo ignorado) e exige senha forte")
    void registrarSenhaFraca() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Hacker", "email": "h@ford.com", "senha": "abc", "role": "ADMIN",
                                 "concessionariaId": %d}""".formatted(concSp.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("senha"));
    }

    @Test
    @DisplayName("e-mail já cadastrado → 409")
    void registrarEmailDuplicado() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Outro", "email": "admin@ford.com", "senha": "Senha1234",
                                 "concessionariaId": %d}""".formatted(concSp.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("concessionária inexistente → 422")
    void registrarConcessionariaInexistente() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Outro", "email": "outro@ford.com", "senha": "Senha1234",
                                 "concessionariaId": 999999}"""))
                .andExpect(status().isUnprocessableEntity());
    }
}
