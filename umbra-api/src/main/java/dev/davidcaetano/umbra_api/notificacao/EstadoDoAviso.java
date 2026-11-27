package dev.davidcaetano.umbra_api.notificacao;

import java.time.Instant;

public record EstadoDoAviso(long precoAlvoCentavos, Long precoAvisadoCentavos, Instant ultimoAvisoEm,
                            boolean foraDeVenda) {

    boolean armado() {
        return precoAvisadoCentavos == null;
    }
}
