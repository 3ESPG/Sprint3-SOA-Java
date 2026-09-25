package br.com.fiap.fordretention.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Primeiro filtro da aplicação: coloca traceId, ip, método e rota no MDC, então todo log da requisição
 * (inclusive os da camada de segurança) sai correlacionado. O traceId volta no header X-Request-Id.
 * Um X-Request-Id recebido só é reaproveitado se tiver formato seguro (evita log injection).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    private static final Pattern FORMATO_SEGURO = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recebido = request.getHeader(HEADER);
        String traceId = recebido != null && FORMATO_SEGURO.matcher(recebido).matches()
                ? recebido
                : UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        MDC.put("ip", request.getRemoteAddr());
        MDC.put("metodo", request.getMethod());
        MDC.put("rota", request.getRequestURI());
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
