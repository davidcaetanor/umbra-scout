package dev.davidcaetano.umbra_api.usuario.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-13T15:00:00Z");
    private static final SecretKey CHAVE =
            new SecretKeySpec("k".repeat(32).getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final JwtService jwtService = new JwtService(
            NimbusJwtEncoder.withSecretKey(CHAVE).algorithm(MacAlgorithm.HS256).build(), "umbra-api",
            Duration.ofHours(6), Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void emitir_deveAssinarComEmissorAssuntoEValidade() {
        TokenEmitido token = jwtService.emitir(42L);

        Jwt jwt = NimbusJwtDecoder.withSecretKey(CHAVE).macAlgorithm(MacAlgorithm.HS256).build().decode(token.valor());
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("umbra-api");
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getIssuedAt()).isEqualTo(AGORA);
        assertThat(jwt.getExpiresAt()).isEqualTo(AGORA.plus(Duration.ofHours(6)));
        assertThat(token.expiraEm()).isEqualTo(AGORA.plus(Duration.ofHours(6)));
    }

    @Test
    void autenticar_deveEntregarOUsuarioDoAssuntoComoPrincipal() {
        Authentication autenticacao = jwtService.autenticar(jwtComAssunto("42"));

        assertThat(autenticacao.isAuthenticated()).isTrue();
        assertThat(autenticacao.getPrincipal()).isEqualTo(new UsuarioAutenticado(42L));
        assertThat(autenticacao.getAuthorities()).isEmpty();
    }

    @Test
    void autenticar_deveRecusarAssuntoQueNaoIdentificaUsuario() {
        assertThatThrownBy(() -> jwtService.autenticar(jwtComAssunto("abc")))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void autenticar_deveRecusarTokenSemAssunto() {
        assertThatThrownBy(() -> jwtService.autenticar(jwtComAssunto(null)))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    private static Jwt jwtComAssunto(String assunto) {
        Jwt.Builder construtor = Jwt.withTokenValue("valor").header("alg", "HS256").claim("iss", "umbra-api");
        if (assunto != null) {
            construtor.subject(assunto);
        }
        return construtor.build();
    }
}
