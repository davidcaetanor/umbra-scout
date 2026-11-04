package dev.davidcaetano.umbra_api.catalogo.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificacaoDePrecoTest {

    private static final Instant AGORA = Instant.parse("2026-11-03T12:00:00Z");

    @Test
    void gratuito_valorZeroSemOriginal_eGratuito() {
        assertThat(ClassificacaoDePreco.gratuito(0, null)).isTrue();
    }

    @Test
    void gratuito_valorZeroComOriginalZero_eGratuito() {
        assertThat(ClassificacaoDePreco.gratuito(0, 0L)).isTrue();
    }

    @Test
    void gratuito_valorZeroComOriginalMaiorQueZero_ePromocaoDe100PorCentoENaoGratuito() {
        assertThat(ClassificacaoDePreco.gratuito(0, 5000L)).isFalse();
    }

    @Test
    void gratuito_valorPositivo_naoEGratuito() {
        assertThat(ClassificacaoDePreco.gratuito(1, null)).isFalse();
    }

    @Test
    void promocaoEncerrada_semExpiracao_naoEstaEncerrada() {
        assertThat(ClassificacaoDePreco.promocaoEncerrada(null, AGORA)).isFalse();
    }

    @Test
    void promocaoEncerrada_expiracaoUmSegundoAntes_estaEncerrada() {
        assertThat(ClassificacaoDePreco.promocaoEncerrada(AGORA.minus(Duration.ofSeconds(1)), AGORA)).isTrue();
    }

    @Test
    void promocaoEncerrada_expiracaoUmSegundoDepois_naoEstaEncerrada() {
        assertThat(ClassificacaoDePreco.promocaoEncerrada(AGORA.plus(Duration.ofSeconds(1)), AGORA)).isFalse();
    }

    @Test
    void promocaoEncerrada_expiracaoExatamenteAgora_naoEstaEncerrada() {
        assertThat(ClassificacaoDePreco.promocaoEncerrada(AGORA, AGORA)).isFalse();
    }
}
