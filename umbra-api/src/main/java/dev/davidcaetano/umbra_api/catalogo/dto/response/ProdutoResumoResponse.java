package dev.davidcaetano.umbra_api.catalogo.dto.response;

import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import org.jspecify.annotations.Nullable;

public record ProdutoResumoResponse(
        Long id,
        String nome,
        TipoProduto tipo,
        @Nullable String categoria,
        @Nullable String imagemUrl,
        @Nullable OfertaResponse melhorPreco) {
}
