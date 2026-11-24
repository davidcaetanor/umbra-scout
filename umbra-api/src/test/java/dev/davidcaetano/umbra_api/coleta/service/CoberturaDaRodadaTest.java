package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.coleta.Cobertura;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CoberturaDaRodadaTest {

    private static final VereditoRodada SAUDAVEL = new VereditoRodada(Set.of());
    private static final VereditoRodada DEGRADADA = new VereditoRodada(Set.of(CondicaoDeDrift.COBERTURA_ABAIXO_DO_DECLARADO));

    @Test
    void de_censoSaudavelComDistintosIguaisAoDeclarado_eCompleta() {
        assertThat(CoberturaDaRodada.de(censo(100L, 100, 0), SAUDAVEL)).isEqualTo(CoberturaDaRodada.COMPLETA);
    }

    @Test
    void de_censoSaudavelSemTotalDeclarado_eCompleta() {
        assertThat(CoberturaDaRodada.de(censo(null, 100, 0), SAUDAVEL)).isEqualTo(CoberturaDaRodada.COMPLETA);
    }

    @Test
    void de_censoSaudavelComDistintosAbaixoDoDeclarado_estaDentroDaMargem() {
        assertThat(CoberturaDaRodada.de(censo(101L, 100, 0), SAUDAVEL)).isEqualTo(CoberturaDaRodada.DENTRO_DA_MARGEM);
    }

    @Test
    void de_censoDegradado_eInsuficiente() {
        assertThat(CoberturaDaRodada.de(censo(100L, 100, 0), DEGRADADA)).isEqualTo(CoberturaDaRodada.INSUFICIENTE);
    }

    @Test
    void de_censoSaudavelComItemSemPreco_eInsuficiente() {
        assertThat(CoberturaDaRodada.de(censo(100L, 100, 1), SAUDAVEL)).isEqualTo(CoberturaDaRodada.INSUFICIENTE);
    }

    @Test
    void de_amostra_eInsuficiente() {
        ResultadoColeta amostra = new ResultadoColeta(OrigemColeta.STEAM_API, List.of(), 10, 0,
                new Reconciliacao(null, 10, 10), Cobertura.amostra());

        assertThat(CoberturaDaRodada.de(amostra, SAUDAVEL)).isEqualTo(CoberturaDaRodada.INSUFICIENTE);
    }

    private static ResultadoColeta censo(Long declarado, int distintos, int semPreco) {
        return new ResultadoColeta(OrigemColeta.KABUM_API, List.of(), distintos, semPreco,
                new Reconciliacao(declarado, distintos, distintos), Cobertura.censoDe(CodigoLoja.KABUM));
    }
}
