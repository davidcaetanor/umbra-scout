package dev.davidcaetano.umbra_api.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SenhaConfigTest {

    private final PasswordEncoder encoder = new SenhaConfig().passwordEncoder();

    @Test
    void deveGerarHashBcryptComCusto12() {
        String hash = encoder.encode("senha-de-teste");

        assertThat(hash).startsWith("$2a$12$").hasSize(60);
        assertThat(encoder.matches("senha-de-teste", hash)).isTrue();
        assertThat(encoder.matches("outra-senha", hash)).isFalse();
    }

    @Test
    void deveGerarHashDiferenteParaAMesmaSenha() {
        assertThat(encoder.encode("senha-de-teste")).isNotEqualTo(encoder.encode("senha-de-teste"));
    }
}
