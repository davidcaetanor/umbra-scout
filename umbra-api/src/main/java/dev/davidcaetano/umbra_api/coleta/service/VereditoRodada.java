package dev.davidcaetano.umbra_api.coleta.service;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record VereditoRodada(Set<CondicaoDeDrift> condicoes) {

    public VereditoRodada {
        Set<CondicaoDeDrift> emOrdemDeDeclaracao = EnumSet.noneOf(CondicaoDeDrift.class);
        emOrdemDeDeclaracao.addAll(condicoes);
        condicoes = Collections.unmodifiableSet(emOrdemDeDeclaracao);
    }

    public SituacaoRodada situacao() {
        if (condicoes.isEmpty()) {
            return SituacaoRodada.SAUDAVEL;
        }

        boolean algumaRejeita = condicoes.stream()
                .anyMatch(condicao -> condicao.efeito() == SituacaoRodada.REJEITADA);

        return algumaRejeita ? SituacaoRodada.REJEITADA : SituacaoRodada.DEGRADADA;
    }
}
