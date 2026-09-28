package dev.davidcaetano.umbra_api.coleta.service;

import java.util.Set;

public record VereditoRodada(Set<CondicaoDeDrift> condicoes) {

    public VereditoRodada {
        condicoes = Set.copyOf(condicoes);
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
