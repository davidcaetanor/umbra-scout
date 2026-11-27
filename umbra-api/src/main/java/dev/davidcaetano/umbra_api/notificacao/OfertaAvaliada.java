package dev.davidcaetano.umbra_api.notificacao;

import dev.davidcaetano.umbra_api.catalogo.enums.SituacaoDoPreco;

import java.util.Objects;

public record OfertaAvaliada(long ofertaId, long valorCentavos, SituacaoDoPreco situacao) {

    public OfertaAvaliada {
        Objects.requireNonNull(situacao, "situacao é obrigatória");
    }

    boolean valeParaAviso() {
        return situacao.valeParaAviso();
    }
}
