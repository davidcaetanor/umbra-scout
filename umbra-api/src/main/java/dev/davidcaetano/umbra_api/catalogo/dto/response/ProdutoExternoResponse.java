package dev.davidcaetano.umbra_api.catalogo.dto.response;

public record ProdutoExternoResponse(
        String gid,
        String nome,
        String imagemUrl,
        OfertaResponse melhorPreco) {
}
