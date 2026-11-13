package dev.davidcaetano.umbra_api.config;

import dev.davidcaetano.umbra_api.config.seguranca.ProblemDetailAccessDeniedHandler;
import dev.davidcaetano.umbra_api.config.seguranca.ProblemDetailAuthenticationEntryPoint;
import dev.davidcaetano.umbra_api.usuario.service.JwtService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Configuration
public class SecurityConfig {

    static final String EMISSOR_DO_TOKEN = "umbra-api";
    private static final MacAlgorithm ALGORITMO_DO_TOKEN = MacAlgorithm.HS256;
    private static final String CABECALHO_REQUEST_ID = "X-Request-Id";
    private static final Duration CACHE_DO_PREFLIGHT = Duration.ofHours(1);

    private final SecretKey chaveDoToken;
    private final Duration validadeDoToken;
    private final List<String> origensPermitidas;

    public SecurityConfig(UmbraProperties umbraProperties) {
        this.validadeDoToken = Duration.ofHours(umbraProperties.jwt().ttlHoras());
        this.origensPermitidas = List.copyOf(umbraProperties.cors().origens());
        this.chaveDoToken = new SecretKeySpec(umbraProperties.jwt().secret().getBytes(StandardCharsets.UTF_8),
                ALGORITMO_DO_TOKEN.getName());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        var entryPoint = new ProblemDetailAuthenticationEntryPoint(handlerExceptionResolver);
        var accessDeniedHandler = new ProblemDetailAccessDeniedHandler(handlerExceptionResolver);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/auth/cadastro", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/produtos/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.decoder(jwtDecoder()).jwtAuthenticationConverter(jwtService::autenticar))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(excecao -> excecao
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origensPermitidas);
        cors.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name()));
        cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, CABECALHO_REQUEST_ID));
        cors.setExposedHeaders(List.of(CABECALHO_REQUEST_ID));
        cors.setAllowCredentials(false);
        cors.setMaxAge(CACHE_DO_PREFLIGHT);

        UrlBasedCorsConfigurationSource origem = new UrlBasedCorsConfigurationSource();
        origem.registerCorsConfiguration("/**", cors);
        return origem;
    }

    @Bean
    public JwtService jwtService(JwtEncoder jwtEncoder, Clock clock) {
        return new JwtService(jwtEncoder, EMISSOR_DO_TOKEN, validadeDoToken, clock);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveDoToken)
                .macAlgorithm(ALGORITMO_DO_TOKEN)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISSOR_DO_TOKEN));
        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return NimbusJwtEncoder.withSecretKey(chaveDoToken)
                .algorithm(ALGORITMO_DO_TOKEN)
                .build();
    }
}
