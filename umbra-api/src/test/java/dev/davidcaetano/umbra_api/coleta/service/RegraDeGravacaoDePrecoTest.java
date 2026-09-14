package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.coleta.service.RegraDeGravacaoDePreco.UltimoPreco;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RegraDeGravacaoDePrecoTest {

    private static final Instant AGORA = Instant.parse("2026-09-14T09:00:00Z");
    private static final Instant UMA_HORA_ATRAS = AGORA.minusSeconds(3600);

    private final RegraDeGravacaoDePreco regra = new RegraDeGravacaoDePreco();

    @Test
    void deveGravarQuandoOfertaForNova() {
        UltimoPreco ultimo = new UltimoPreco(1000L, true, UMA_HORA_ATRAS);

        assertThat(regra.devePersistir(true, ultimo, 1000L, true, AGORA)).isTrue();
    }

    @Test
    void deveGravarQuandoNaoHouverRegistroAnterior() {
        assertThat(regra.devePersistir(false, null, 1000L, true, AGORA)).isTrue();
    }

    @Test
    void deveGravarQuandoValorMudou() {
        UltimoPreco ultimo = new UltimoPreco(1000L, true, UMA_HORA_ATRAS);

        assertThat(regra.devePersistir(false, ultimo, 900L, true, AGORA)).isTrue();
    }

    @Test
    void deveGravarQuandoDisponibilidadeMudou() {
        UltimoPreco ultimo = new UltimoPreco(1000L, true, UMA_HORA_ATRAS);

        assertThat(regra.devePersistir(false, ultimo, 1000L, false, AGORA)).isTrue();
    }

    @Test
    void naoDeveGravarQuandoNadaMudou() {
        UltimoPreco ultimo = new UltimoPreco(1000L, true, UMA_HORA_ATRAS);

        assertThat(regra.devePersistir(false, ultimo, 1000L, true, AGORA)).isFalse();
    }

    @Test
    void naoDeveGravarQuandoUltimoRegistroTiverExatos89Dias() {
        UltimoPreco ultimo = new UltimoPreco(1000L, true, AGORA.minus(Duration.ofDays(89)));

        assertThat(regra.devePersistir(false, ultimo, 1000L, true, AGORA)).isFalse();
    }

    @Test
    void deveGravarQuandoUltimoRegistroTiver91Dias() {
        UltimoPreco ultimo = new UltimoPreco(1000L, true, AGORA.minus(Duration.ofDays(91)));

        assertThat(regra.devePersistir(false, ultimo, 1000L, true, AGORA)).isTrue();
    }
}
