package br.com.fiap.fordretention;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FordRetentionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FordRetentionApplication.class, args);
    }
}
