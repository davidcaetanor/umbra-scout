package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;

import java.time.OffsetDateTime;

public record ProdutoColetado(
        CodigoLoja loja,
        String identificadorLoja,
        TipoProduto tipo,
        String nome,
        String categoria,
        String url,
        String imagemUrl,
        String chaveItad,
        long valorCentavos,
        Long valorOriginalCentavos,
        Short descontoPct,
        boolean disponivel,
        OrigemColeta origemColeta,
        OffsetDateTime coletadoEm,
        OffsetDateTime expiry
) {
}
