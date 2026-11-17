package dev.davidcaetano.umbra_api.config.openapi.dto.response;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record ExemploResponse(
        long id,
        String nome,
        @Nullable String apelido,
        @Nullable ItemResponse destaque,
        ItemResponse principal,
        List<ItemResponse> itens) {

    public record ItemResponse(String codigo) {
    }
}
