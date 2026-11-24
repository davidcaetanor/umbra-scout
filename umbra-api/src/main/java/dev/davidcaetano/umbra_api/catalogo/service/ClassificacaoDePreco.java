package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.enums.SituacaoDoPreco;

import java.time.Duration;
import java.time.Instant;

public final class ClassificacaoDePreco {

    static final Duration LIMITE_DE_VERIFICACAO = Duration.ofHours(24);

    private ClassificacaoDePreco() {
    }

    public static boolean gratuito(long valorCentavos, Long valorOriginalCentavos) {
        return valorCentavos == 0 && (valorOriginalCentavos == null || valorOriginalCentavos == 0);
    }

    public static SituacaoDoPreco situacao(Instant expiraEm, Instant verificadoEm, Instant agora) {
        if (expiraEm != null && !expiraEm.isAfter(agora) && verificadoEm.isBefore(expiraEm)) {
            return SituacaoDoPreco.PROMOCAO_VENCIDA_SEM_COLETA;
        }
        if (expiraEm != null && expiraEm.isAfter(agora)) {
            return SituacaoDoPreco.PROMOCAO_NO_PRAZO;
        }
        if (verificadoEm.isBefore(agora.minus(LIMITE_DE_VERIFICACAO))) {
            return SituacaoDoPreco.VERIFICACAO_ATRASADA;
        }
        return SituacaoDoPreco.VIGENTE;
    }
}
