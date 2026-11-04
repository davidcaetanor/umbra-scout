package dev.davidcaetano.umbra_api.catalogo.repository;

import java.time.Instant;

public interface OfertaAtualProjecao {

    String getLoja();

    String getUrl();

    long getValorCentavos();

    Long getValorOriginalCentavos();

    Short getDescontoPct();

    Instant getExpiraEm();

    Instant getColetadoEm();
}
