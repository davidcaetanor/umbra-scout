package dev.davidcaetano.umbra_api.catalogo.dto.response;

import java.util.List;

public record BuscaProdutosResponse(
        List<ProdutoResumoResponse> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        List<ProdutoExternoResponse> externos) {
}
