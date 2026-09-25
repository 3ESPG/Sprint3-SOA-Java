package br.com.fiap.fordretention.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Actuator expõe só health e prometheus; o resto (env, beans, heapdump...) fica fechado. */
@AutoConfigureObservability
class ActuatorIT extends IntegrationTestSupport {

    @Test
    @DisplayName("/actuator/prometheus publica métricas HTTP")
    void prometheus() throws Exception {
        mvc.perform(get("/leads").with(tokenDe(admin))).andExpect(status().isOk());

        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("http_server_requests_seconds")));
    }

    @Test
    @DisplayName("demais endpoints do Actuator negados até para ADMIN → 403")
    void demaisFechados() throws Exception {
        mvc.perform(get("/actuator/env").with(tokenDe(admin))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/heapdump").with(tokenDe(admin))).andExpect(status().isForbidden());
    }
}
