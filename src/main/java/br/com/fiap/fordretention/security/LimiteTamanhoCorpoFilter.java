package br.com.fiap.fordretention.security;

import br.com.fiap.fordretention.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Recusa com 413 corpos maiores que o limite antes de o Jackson tentar desserializá-los
 * (os payloads da API têm poucos KB). Complementa o rate limiting contra consumo de recursos.
 */
public class LimiteTamanhoCorpoFilter extends OncePerRequestFilter {

    private final long limiteBytes;
    private final ObjectMapper objectMapper;

    public LimiteTamanhoCorpoFilter(long limiteBytes, ObjectMapper objectMapper) {
        this.limiteBytes = limiteBytes;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > limiteBytes) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(), ApiError.of(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Corpo da requisição excede o limite de %d KB".formatted(limiteBytes / 1024),
                    request.getRequestURI()));
            return;
        }
        chain.doFilter(request, response);
    }
}
