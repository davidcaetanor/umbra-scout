package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;

import java.time.Instant;

public interface ProdutoResumoProjecao {

    Long getId();

    String getNome();

    TipoProduto getTipo();

    String getCategoria();

    String getImagemUrl();

    String getLoja();

    String getUrl();

    Long getValorCentavos();

    Long getValorOriginalCentavos();

    Short getDescontoPct();

    Instant getExpiraEm();

    Instant getColetadoEm();
}
