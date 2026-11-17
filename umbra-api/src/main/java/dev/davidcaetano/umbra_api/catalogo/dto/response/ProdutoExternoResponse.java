package dev.davidcaetano.umbra_api.catalogo.dto.response;

import org.jspecify.annotations.Nullable;

public record ProdutoExternoResponse(
        String gid,
        String nome,
        @Nullable String imagemUrl,
        OfertaResponse melhorPreco) {
}
