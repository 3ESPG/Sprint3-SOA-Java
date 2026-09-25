package br.com.fiap.fordretention.security;

import br.com.fiap.fordretention.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 401 no formato padrão da API: sem token, token inválido ou expirado. */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object erroJwt = request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERRO_JWT);
        String mensagem = erroJwt != null
                ? erroJwt.toString()
                : "Autenticação necessária: envie o header Authorization: Bearer <token>";

        log.atWarn()
                .addKeyValue("evento", erroJwt != null ? "auth.token_invalido" : "auth.sem_token")
                .addKeyValue("motivo", erroJwt != null ? erroJwt.toString() : "ausente")
                .log("Requisição não autenticada");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("WWW-Authenticate", "Bearer");
        objectMapper.writeValue(response.getOutputStream(),
                ApiError.of(HttpStatus.UNAUTHORIZED, mensagem, request.getRequestURI()));
    }
}
