package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroDeJogoExternoTest {

    private static final OfertaExterna NUUVEM = oferta(CodigoLoja.NUUVEM, 24705, (short) 10);
    private static final OfertaExterna STEAM = oferta(CodigoLoja.STEAM, 27450, (short) 0);
    private static final JogoExterno ELDEN_RING = new JogoExterno("gid", "Elden Ring", null, List.of(NUUVEM, STEAM));

    @Test
    void semFiltro_mantemOJogoComTodasAsOfertas() {
        assertThat(FiltroDeJogoExterno.aplicar(ELDEN_RING, filtro(null, null, null, null)))
                .hasValueSatisfying(jogo -> assertThat(jogo.ofertas()).containsExactly(NUUVEM, STEAM));
    }

    @Test
    void comLoja_ficaSoComAsOfertasDaLoja() {
        assertThat(FiltroDeJogoExterno.aplicar(ELDEN_RING, filtro(CodigoLoja.STEAM, null, null, null)))
                .hasValueSatisfying(jogo -> assertThat(jogo.ofertas()).containsExactly(STEAM));
    }

    @Test
    void lojaSemOfertaDoJogo_descartaOJogo() {
        assertThat(FiltroDeJogoExterno.aplicar(ELDEN_RING, filtro(CodigoLoja.KABUM, null, null, null))).isEmpty();
    }

    @Test
    void faixaDePreco_valeSobreOMelhorPrecoConsiderado() {
        assertThat(FiltroDeJogoExterno.aplicar(ELDEN_RING, filtro(null, null, 25000L, null))).isPresent();
        assertThat(FiltroDeJogoExterno.aplicar(ELDEN_RING, filtro(null, 25000L, null, null))).isEmpty();
        assertThat(FiltroDeJogoExterno.aplicar(ELDEN_RING, filtro(CodigoLoja.STEAM, null, 25000L, null))).isEmpty();
    }

    @Test
    void descontoMinimo_valeSobreOMelhorPrecoConsiderado() {
        assertThat(FiltroDeJogoExterno.aplicar(ELDEN_RING, filtro(null, null, null, 10))).isPresent();
        assertThat(FiltroDeJogoExterno.aplicar(ELDEN_RING, filtro(null, null, null, 11))).isEmpty();
        assertThat(FiltroDeJogoExterno.aplicar(ELDEN_RING, filtro(CodigoLoja.STEAM, null, null, 10))).isEmpty();
    }

    private static FiltroDoCatalogo filtro(CodigoLoja loja, Long precoMin, Long precoMax, Integer descontoMin) {
        return new FiltroDoCatalogo("elden", null, null, precoMin, precoMax, descontoMin, loja);
    }

    private static OfertaExterna oferta(CodigoLoja loja, long valor, short desconto) {
        return new OfertaExterna(loja, "https://loja.exemplo", valor, null, desconto, null);
    }
}
