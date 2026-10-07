package dev.davidcaetano.umbra_api.catalogo.repository;

import java.time.Instant;

public interface OfertaComUltimoPrecoProjecao {

    Long getOfertaId();

    String getCodigoLoja();

    String getIdentificadorLoja();

    String getChaveItad();

    String getNomeProduto();

    long getValorCentavos();

    boolean isDisponivel();

    Instant getColetadoEm();
}
