package dev.davidcaetano.umbra_api.notificacao;

import java.util.List;
import java.util.Objects;

public record AlvoEmEnvio(long produtoId, String nome, long precoAlvoCentavos, boolean baixouDeNovo,
                          List<Long> avisoIds, List<LojaEmEnvio> lojas) {

    public AlvoEmEnvio {
        Objects.requireNonNull(nome, "nome é obrigatório");
        avisoIds = List.copyOf(avisoIds);
        lojas = List.copyOf(lojas);
    }
}
