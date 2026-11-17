package dev.davidcaetano.umbra_api.catalogo.dto.response;

import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record ProdutoResponse(
        Long id,
        String nome,
        TipoProduto tipo,
        @Nullable String categoria,
        @Nullable String imagemUrl,
        @Nullable OfertaResponse melhorPreco,
        List<OfertaResponse> ofertas) {
}
