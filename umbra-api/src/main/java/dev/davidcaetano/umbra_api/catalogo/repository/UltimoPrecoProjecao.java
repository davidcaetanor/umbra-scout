package dev.davidcaetano.umbra_api.catalogo.repository;

import java.time.Instant;

public interface UltimoPrecoProjecao {

    Long getOfertaId();

    long getValorCentavos();

    boolean isDisponivel();

    Instant getColetadoEm();
}
