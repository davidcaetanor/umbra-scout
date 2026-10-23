package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.coleta.properties.GravacaoPrecoProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
final class RegraDeGravacaoDePreco {

    private final GravacaoPrecoProperties gravacao;

    RegraDeGravacaoDePreco(GravacaoPrecoProperties gravacao) {
        this.gravacao = gravacao;
    }

    boolean devePersistir(boolean ofertaNova, UltimoPreco ultimoPreco, long valorCentavos,
                           boolean disponivel, Instant agora) {

        if (ofertaNova || ultimoPreco == null) {
            return true;
        }

        boolean valorMudou = ultimoPreco.valorCentavos() != valorCentavos;
        boolean disponibilidadeMudou = ultimoPreco.disponivel() != disponivel;
        boolean janelaDeVivacidadeExpirou = ultimoPreco.coletadoEm()
                .isBefore(agora.minus(gravacao.janelaVivacidadeDias(), ChronoUnit.DAYS));

        return valorMudou || disponibilidadeMudou || janelaDeVivacidadeExpirou;
    }
}
