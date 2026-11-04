package dev.davidcaetano.umbra_api.catalogo.dto.response;

import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;

public record ProdutoResumoResponse(
        Long id,
        String nome,
        TipoProduto tipo,
        String categoria,
        String imagemUrl,
        OfertaResponse melhorPreco) {
}
