package br.com.fiap.fordretention.security;

import br.com.fiap.fordretention.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting com Bucket4j (token bucket), executado depois do {@link JwtAuthenticationFilter}:
 * <ul>
 *   <li>POST /auth/login: {@code loginPorMinuto} por IP;</li>
 *   <li>requisições autenticadas: {@code autenticadoPorMinuto} por usuário (uid do JWT);</li>
 *   <li>demais requisições anônimas: {@code anonimoPorMinuto} por IP.</li>
 * </ul>
 * Ao exceder, responde 429 no formato padrão da API com o header Retry-After (segundos).
 * <p>
 * O IP vem de {@code request.getRemoteAddr()}: o header X-Forwarded-For só é considerado quando
 * {@code server.forward-headers-strategy} está ativo atrás de um proxy confiável, para que o cliente
 * não consiga trocar de "IP" a cada tentativa. Os buckets ficam em memória (uma instância);
 * com várias réplicas o próximo passo é o bucket4j-redis.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final Duration JANELA = Duration.ofMinutes(1);
    /** Proteção simples contra crescimento ilimitado do mapa (ex.: varredura com muitos IPs). */
    private static final int MAX_CHAVES = 10_000;

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.habilitado() || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Limite limite = limiteDa(request);
        if (buckets.size() > MAX_CHAVES) {
            buckets.clear();
        }
        Bucket bucket = buckets.computeIfAbsent(limite.chave(), k -> novoBucket(limite.porMinuto()));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        log.atWarn()
                .addKeyValue("evento", "rate_limit.excedido")
                .addKeyValue("chave", limite.tipo())
                .addKeyValue("ip", request.getRemoteAddr())
                .addKeyValue("metodo", request.getMethod())
                .addKeyValue("rota", request.getRequestURI())
                .addKeyValue("limitePorMinuto", limite.porMinuto())
                .log("Limite de requisições excedido");

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(HttpStatus.TOO_MANY_REQUESTS,
                "Muitas requisições. Tente novamente em %d segundos".formatted(retryAfter),
                request.getRequestURI()));
    }

    private Limite limiteDa(HttpServletRequest request) {
        if (HttpMethod.POST.matches(request.getMethod()) && "/auth/login".equals(request.getRequestURI())) {
            return new Limite("login", "login:" + request.getRemoteAddr(), properties.loginPorMinuto());
        }
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return new Limite("usuario", "usuario:" + usuario.id(), properties.autenticadoPorMinuto());
        }
        return new Limite("anonimo", "anonimo:" + request.getRemoteAddr(), properties.anonimoPorMinuto());
    }

    private static Bucket novoBucket(int porMinuto) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(porMinuto).refillGreedy(porMinuto, JANELA).build())
                .build();
    }

    private record Limite(String tipo, String chave, int porMinuto) {
    }
}
