package dev.davidcaetano.umbra_api.catalogo.repository;

import java.time.Instant;

public interface OfertaComUltimoPrecoProjecao {

    Long getOfertaId();

    String getCodigoLoja();

    String getIdentificadorLoja();

    long getValorCentavos();

    boolean isDisponivel();

    Instant getColetadoEm();
}
