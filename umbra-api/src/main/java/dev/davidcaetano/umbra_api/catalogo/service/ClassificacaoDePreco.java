package dev.davidcaetano.umbra_api.catalogo.service;

import java.time.Instant;

public final class ClassificacaoDePreco {

    private ClassificacaoDePreco() {
    }

    public static boolean gratuito(long valorCentavos, Long valorOriginalCentavos) {
        return valorCentavos == 0 && (valorOriginalCentavos == null || valorOriginalCentavos == 0);
    }

    public static boolean promocaoEncerrada(Instant expiraEm, Instant agora) {
        return expiraEm != null && expiraEm.isBefore(agora);
    }
}
