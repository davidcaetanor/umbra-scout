package dev.davidcaetano.umbra_api.catalogo.repository;

import java.time.Instant;

public interface PontoDePrecoProjecao {

    Long getId();

    Long getOfertaId();

    String getLoja();

    long getValorCentavos();

    Long getValorOriginalCentavos();

    boolean isDisponivel();

    Instant getColetadoEm();
}
