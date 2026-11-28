package dev.davidcaetano.umbra_api.notificacao;

import java.util.Objects;

public record ItemDoEmail(long produtoId, String nome, long precoCentavos, String loja, long precoAlvoCentavos,
                          int outrasLojas, boolean baixouDeNovo) {

    public ItemDoEmail {
        Objects.requireNonNull(nome, "nome é obrigatório");
        Objects.requireNonNull(loja, "loja é obrigatória");
    }
}
