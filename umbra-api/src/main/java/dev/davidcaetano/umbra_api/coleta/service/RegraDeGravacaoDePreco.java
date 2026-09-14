package dev.davidcaetano.umbra_api.coleta.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class RegraDeGravacaoDePreco {

    static final int JANELA_VIVACIDADE_DIAS = 90;

    public boolean devePersistir(boolean ofertaNova, UltimoPreco ultimoPreco, long valorCentavos,
                                  boolean disponivel, Instant agora) {

        if (ofertaNova || ultimoPreco == null) {
            return true;
        }

        boolean valorMudou = ultimoPreco.valorCentavos() != valorCentavos;
        boolean disponibilidadeMudou = ultimoPreco.disponivel() != disponivel;
        boolean janelaDeVivacidadeExpirou = ultimoPreco.coletadoEm()
                .isBefore(agora.minus(JANELA_VIVACIDADE_DIAS, ChronoUnit.DAYS));

        return valorMudou || disponibilidadeMudou || janelaDeVivacidadeExpirou;
    }

    public record UltimoPreco(long valorCentavos, boolean disponivel, Instant coletadoEm) {
    }
}
