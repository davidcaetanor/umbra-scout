package dev.davidcaetano.umbra_api.coleta;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultadoColetaTest {

    @Test
    void deveRejeitarReconciliacaoNula() {
        List<OfertaColetada> ofertas = List.of();

        assertThatThrownBy(() -> new ResultadoColeta(ofertas, 0, 0, null))
                .isInstanceOf(NullPointerException.class);
    }
}
