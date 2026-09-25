package br.com.fiap.fordretention.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** Relógio injetável: permite testes determinísticos de datas, idades de veículos e expiração do JWT. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
