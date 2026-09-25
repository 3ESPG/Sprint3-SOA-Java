package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.model.enums.StatusLead;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServicoControllerIT extends IntegrationTestSupport {

    private String corpo(long veiculoId, long concessionariaId, String tipo, LocalDate data, boolean pago) {
        return """
                {"veiculoId": %d, "concessionariaId": %d, "tipo": "%s", "valor": 980.00,
                 "data": "%s", "pago": %s}""".formatted(veiculoId, concessionariaId, tipo, data, pago);
    }

    @Test
    @DisplayName("serviço pago → 201 e o lead ativo do veículo vira CONVERTIDO")
    void servicoPagoConverteLead() throws Exception {
        mvc.perform(post("/servicos").with(tokenDe(gestorSp)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(rangerAntigaMaria.getId(), concSp.getId(), "REVISAO", LocalDate.now(), true)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        assertThat(leadRepository.findById(leadMaria.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusLead.CONVERTIDO);
    }

    @Test
    @DisplayName("data futura → 422; RECALL pago → 422")
    void regrasDeNegocio() throws Exception {
        mvc.perform(post("/servicos").with(tokenDe(gestorSp)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(rangerAntigaMaria.getId(), concSp.getId(), "REVISAO",
                                LocalDate.now().plusDays(3), true)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("A data do serviço não pode ser futura"));

        mvc.perform(post("/servicos").with(tokenDe(gestorSp)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(rangerAntigaMaria.getId(), concSp.getId(), "RECALL", LocalDate.now(), true)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("veículo inexistente no payload → 422; campos obrigatórios ausentes → 400")
    void referenciasEValidacao() throws Exception {
        mvc.perform(post("/servicos").with(tokenDe(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(999_999, concSp.getId(), "REVISAO", LocalDate.now(), true)))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(post("/servicos").with(tokenDe(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(6)));
    }

    @Test
    @DisplayName("gestor de SP registrando serviço no RJ → 403")
    void outraConcessionaria() throws Exception {
        mvc.perform(post("/servicos").with(tokenDe(gestorSp)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(kaFernanda.getId(), concRj.getId(), "REVISAO", LocalDate.now(), true)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /servicos?tipo=REVISAO&pago=true")
    void listarComFiltros() throws Exception {
        mvc.perform(get("/servicos").param("tipo", "REVISAO").param("pago", "true").with(tokenDe(consultorSp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].modelo").value("Territory"));
    }
}
