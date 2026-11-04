package dev.davidcaetano.umbra_api.catalogo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodoHistoricoTest {

    private static final Instant AGORA = Instant.parse("2026-11-04T12:00:00Z");

    @Test
    void deCodigo_deveDevolverAConstanteDeCadaCodigo() {
        assertThat(PeriodoHistorico.deCodigo("30d")).isEqualTo(PeriodoHistorico.TRINTA_DIAS);
        assertThat(PeriodoHistorico.deCodigo("90d")).isEqualTo(PeriodoHistorico.NOVENTA_DIAS);
        assertThat(PeriodoHistorico.deCodigo("1a")).isEqualTo(PeriodoHistorico.UM_ANO);
        assertThat(PeriodoHistorico.deCodigo("tudo")).isEqualTo(PeriodoHistorico.TUDO);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"90D", "NOVENTA_DIAS"})
    void deCodigo_deveRecusarOQueNaoEstaNaListaBranca(String codigo) {
        assertThatThrownBy(() -> PeriodoHistorico.deCodigo(codigo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Período desconhecido");
    }

    @Test
    void inicio_deveRecuarAJanelaDeCadaPeriodo() {
        assertThat(PeriodoHistorico.TRINTA_DIAS.inicio(AGORA)).hasValue(Instant.parse("2026-10-05T12:00:00Z"));
        assertThat(PeriodoHistorico.NOVENTA_DIAS.inicio(AGORA)).hasValue(Instant.parse("2026-08-06T12:00:00Z"));
        assertThat(PeriodoHistorico.UM_ANO.inicio(AGORA)).hasValue(Instant.parse("2025-11-04T12:00:00Z"));
    }

    @Test
    void inicio_deTudoDeveSerVazio() {
        assertThat(PeriodoHistorico.TUDO.inicio(AGORA)).isEmpty();
    }

    @Test
    void toString_deveDevolverOCodigo() {
        assertThat(PeriodoHistorico.TRINTA_DIAS).hasToString("30d");
        assertThat(PeriodoHistorico.NOVENTA_DIAS).hasToString("90d");
        assertThat(PeriodoHistorico.UM_ANO).hasToString("1a");
        assertThat(PeriodoHistorico.TUDO).hasToString("tudo");
    }
}
