package dev.davidcaetano.umbra_api.catalogo.dto.response;

import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;

import java.util.List;

public record ProdutoResponse(
        Long id,
        String nome,
        TipoProduto tipo,
        String categoria,
        String imagemUrl,
        OfertaResponse melhorPreco,
        List<OfertaResponse> ofertas) {
}
