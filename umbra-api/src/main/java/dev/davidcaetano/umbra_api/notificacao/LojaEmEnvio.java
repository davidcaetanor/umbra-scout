package dev.davidcaetano.umbra_api.notificacao;

import dev.davidcaetano.umbra_api.catalogo.enums.SituacaoDoPreco;

import java.util.Objects;

public record LojaEmEnvio(String loja, long valorCentavos, SituacaoDoPreco situacao) {

    public LojaEmEnvio {
        Objects.requireNonNull(loja, "loja é obrigatória");
        Objects.requireNonNull(situacao, "situacao é obrigatória");
    }

    boolean atende(long precoAlvoCentavos) {
        return situacao.valeParaAviso() && valorCentavos <= precoAlvoCentavos;
    }
}
