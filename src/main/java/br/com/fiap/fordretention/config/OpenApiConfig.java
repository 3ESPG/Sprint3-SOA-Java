package br.com.fiap.fordretention.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI em /swagger-ui.html. Faça login em POST /auth/login, copie o accessToken
 * e clique em "Authorize" para chamar os endpoints protegidos.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Ford Retention AI API",
                version = "1.0.0",
                description = """
                        API do Challenge Ford FIAP 2026 – Desafio 02 (Service Share / VIN Share).
                        Gerencia concessionárias, clientes, veículos e serviços; recebe o perfil de retenção \
                        calculado pelo modelo de ML e gera leads proativos para as concessionárias.

                        **Perfis:** ADMIN (Ford), GESTOR_CONCESSIONARIA (apenas a própria concessionária) \
                        e CONSULTOR (consulta dados e atualiza leads).""",
                contact = @Contact(name = "Equipe Ford Retention AI – FIAP")),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER))
@SecurityScheme(
        name = OpenApiConfig.BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Cole o accessToken retornado por POST /auth/login")
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";
}
