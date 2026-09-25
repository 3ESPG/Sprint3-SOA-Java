package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.model.Lead;
import br.com.fiap.fordretention.model.enums.OrigemLead;
import br.com.fiap.fordretention.model.enums.Prioridade;
import br.com.fiap.fordretention.model.enums.StatusLead;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PerfilClienteControllerIT extends IntegrationTestSupport {

    @Test
    @DisplayName("ML envia ABANDONO com score alto → perfil salvo e lead gerado automaticamente")
    void perfilDeRiscoGeraLead() throws Exception {
        String resposta = mvc.perform(put("/clientes/{id}/perfil", joaoSp.getId()).with(tokenDe(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"perfil": "ABANDONO", "scoreRisco": 0.93}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ABANDONO"))
                .andExpect(jsonPath("$.perfilNome").value("Cliente de Abandono"))
                .andExpect(jsonPath("$.scoreRisco").value(0.93))
                .andExpect(jsonPath("$.leadsGerados", hasSize(1)))
                .andReturn().getResponse().getContentAsString();

        Integer leadId = JsonPath.read(resposta, "$.leadsGerados[0]");
        Lead lead = leadRepository.findById(leadId.longValue()).orElseThrow();
        assertThat(lead.getOrigem()).isEqualTo(OrigemLead.MODELO_ML);
        assertThat(lead.getPrioridade()).isEqualTo(Prioridade.ALTA);
        assertThat(lead.getStatus()).isEqualTo(StatusLead.ABERTO);
        assertThat(lead.getVeiculo().getId()).isEqualTo(territoryNovaJoao.getId());
        assertThat(lead.getConcessionaria().getId()).isEqualTo(concSp.getId());
    }

    @Test
    @DisplayName("veículo que já tem lead ativo não recebe lead duplicado")
    void naoDuplicaLead() throws Exception {
        mvc.perform(put("/clientes/{id}/perfil", mariaSp.getId()).with(tokenDe(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"perfil": "ABANDONO", "scoreRisco": 0.99}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadsGerados", hasSize(0)));
        assertThat(leadRepository.findByVeiculoIdAndStatusIn(rangerAntigaMaria.getId(), StatusLead.ativos()))
                .extracting(Lead::getId).isEqualTo(List.of(leadMaria.getId()));
    }

    @Test
    @DisplayName("perfil FIEL não gera lead")
    void fielNaoGeraLead() throws Exception {
        mvc.perform(put("/clientes/{id}/perfil", joaoSp.getId()).with(tokenDe(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"perfil": "FIEL", "scoreRisco": 0.95}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadsGerados", hasSize(0)));
    }

    @Test
    @DisplayName("score fora de 0..1 ou perfil ausente → 400")
    void payloadInvalido() throws Exception {
        mvc.perform(put("/clientes/{id}/perfil", joaoSp.getId()).with(tokenDe(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scoreRisco": 1.5}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)));
    }

    @Test
    @DisplayName("GESTOR não grava perfil (JWT real) → 403")
    void gestorNaoGrava() throws Exception {
        mvc.perform(put("/clientes/{id}/perfil", joaoSp.getId()).with(tokenDe(gestorSp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"perfil": "FIEL", "scoreRisco": 0.1}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET perfil: calculado → 200; ainda não calculado → 404; cliente inexistente → 404")
    void consultarPerfil() throws Exception {
        mvc.perform(get("/clientes/{id}/perfil", mariaSp.getId()).with(tokenDe(consultorSp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ABANDONO"));
        mvc.perform(get("/clientes/{id}/perfil", joaoSp.getId()).with(tokenDe(consultorSp)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/clientes/{id}/perfil", 999_999).with(tokenDe(admin)))
                .andExpect(status().isNotFound());
    }
}
