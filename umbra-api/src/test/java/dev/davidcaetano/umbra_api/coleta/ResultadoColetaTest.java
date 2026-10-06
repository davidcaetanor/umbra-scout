package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultadoColetaTest {

    @Test
    void deveRejeitarReconciliacaoNula() {
        List<OfertaColetada> ofertas = List.of();
        Cobertura cobertura = Cobertura.amostra();

        assertThatThrownBy(() -> new ResultadoColeta(OrigemColeta.KABUM_API, ofertas, 0, 0, null, cobertura))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveRejeitarFonteNula() {
        List<OfertaColetada> ofertas = List.of();
        Reconciliacao reconciliacao = new Reconciliacao(null, 0, 0);
        Cobertura cobertura = Cobertura.amostra();

        assertThatThrownBy(() -> new ResultadoColeta(null, ofertas, 0, 0, reconciliacao, cobertura))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveRejeitarCoberturaNula() {
        List<OfertaColetada> ofertas = List.of();
        Reconciliacao reconciliacao = new Reconciliacao(null, 0, 0);

        assertThatThrownBy(() -> new ResultadoColeta(OrigemColeta.KABUM_API, ofertas, 0, 0, reconciliacao, null))
                .isInstanceOf(NullPointerException.class);
    }
}
