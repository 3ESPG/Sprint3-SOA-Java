package br.com.fiap.fordretention.config;

import br.com.fiap.fordretention.security.JwtAuthenticationFilter;
import br.com.fiap.fordretention.security.JwtService;
import br.com.fiap.fordretention.security.LimiteTamanhoCorpoFilter;
import br.com.fiap.fordretention.security.RateLimitFilter;
import br.com.fiap.fordretention.security.RateLimitProperties;
import br.com.fiap.fordretention.security.RestAccessDeniedHandler;
import br.com.fiap.fordretention.security.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.unit.DataSize;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Autorização em duas camadas:
 * 1) regras grossas por URL/método aqui no SecurityFilterChain;
 * 2) regras finas com @PreAuthorize nos controllers + escopo por concessionária nos services.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] SWAGGER = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   RateLimitProperties rateLimitProperties,
                                                   ObjectMapper objectMapper,
                                                   @Value("${app.max-request-size:64KB}") DataSize tamanhoMaximoCorpo)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // públicos
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()
                        .requestMatchers(SWAGGER).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // exclusivos da Ford (ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/clientes/*/perfil").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/concessionarias", "/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/concessionarias/**", "/leads/**").hasRole("ADMIN")
                        // indicadores: Ford e gestores
                        .requestMatchers(HttpMethod.GET, "/concessionarias/*/service-share")
                        .hasAnyRole("ADMIN", "GESTOR_CONCESSIONARIA")
                        // todo o resto exige autenticação (regras finas via @PreAuthorize)
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new LimiteTamanhoCorpoFilter(tamanhoMaximoCorpo.toBytes(), objectMapper),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                // depois do JWT (já sabe quem é o usuário) e antes da autorização: 429 barra até o login
                .addFilterBefore(new RateLimitFilter(rateLimitProperties, objectMapper), AuthorizationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Usa o UsuarioDetailsService + PasswordEncoder (BCrypt) registrados no contexto. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** CORS para o dashboard web e o app mobile (Expo/React Native web). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> origensPermitidas) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origensPermitidas);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
