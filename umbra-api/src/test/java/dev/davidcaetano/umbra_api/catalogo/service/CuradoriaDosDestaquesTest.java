package dev.davidcaetano.umbra_api.catalogo.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuradoriaDosDestaquesTest {

    @Test
    void deveRespeitarAOrdemEPularQuemNaoPassaOPortao() {
        CuradoriaDosDestaques curadoria = new CuradoriaDosDestaques(8, 1);

        List<Long> escolhidos = curadoria.escolher(List.of(
                candidato(1, "a", "X", false), candidato(2, "b", "Y", true), candidato(3, "c", "Z", true)));

        assertThat(escolhidos).containsExactly(2L, 3L);
    }

    @Test
    void deveLimitarPorPublicadorSemDiferenciarMaiusculasNemEspacos() {
        CuradoriaDosDestaques curadoria = new CuradoriaDosDestaques(8, 1);

        List<Long> escolhidos = curadoria.escolher(List.of(
                candidato(1, "a", "Hede", true), candidato(2, "b", " HEDE ", true), candidato(3, "c", "Outro", true)));

        assertThat(escolhidos).containsExactly(1L, 3L);
    }

    @Test
    void deveAceitarAteOLimiteDoPublicador() {
        CuradoriaDosDestaques curadoria = new CuradoriaDosDestaques(8, 2);

        List<Long> escolhidos = curadoria.escolher(List.of(
                candidato(1, "a", "Hede", true), candidato(2, "b", "Hede", true), candidato(3, "c", "Hede", true)));

        assertThat(escolhidos).containsExactly(1L, 2L);
    }

    @Test
    void publicadorDesconhecido_naoLimita() {
        CuradoriaDosDestaques curadoria = new CuradoriaDosDestaques(8, 1);

        List<Long> escolhidos = curadoria.escolher(List.of(
                candidato(1, "a", null, true), candidato(2, "b", " ", true), candidato(3, "c", null, true),
                candidato(4, "d", "", true)));

        assertThat(escolhidos).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void deveManterUmaEdicaoPorJogoSemGastarOLimiteDoPublicador() {
        CuradoriaDosDestaques curadoria = new CuradoriaDosDestaques(8, 2);

        List<Long> escolhidos = curadoria.escolher(List.of(
                candidato(1, "zeta", "P", true), candidato(2, "zeta", "P", true), candidato(3, "outro", "P", true)));

        assertThat(escolhidos).containsExactly(1L, 3L);
    }

    @Test
    void devePararNoLimiteDeDestaques() {
        CuradoriaDosDestaques curadoria = new CuradoriaDosDestaques(2, 1);

        List<Long> escolhidos = curadoria.escolher(List.of(
                candidato(1, "a", "A", true), candidato(2, "b", "B", true), candidato(3, "c", "C", true)));

        assertThat(escolhidos).containsExactly(1L, 2L);
    }

    @Test
    void deveRecusarLimitesNaoPositivos() {
        assertThatThrownBy(() -> new CuradoriaDosDestaques(0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CuradoriaDosDestaques(1, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static CandidatoADestaque candidato(long id, String jogo, String publicador, boolean qualificado) {
        return new CandidatoADestaque(id, jogo, publicador, qualificado);
    }
}
