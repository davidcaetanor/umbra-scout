package dev.davidcaetano.umbra_api.monitoramento.repository;

import java.time.Instant;

public interface OfertaEmAvaliacaoProjecao {

    Long getMonitoramentoId();

    long getPrecoAlvoCentavos();

    Long getPrecoAvisadoCentavos();

    Instant getUltimoAvisoEm();

    boolean isForaDeVenda();

    Long getOfertaId();

    long getValorCentavos();

    Instant getExpiraEm();

    Instant getVerificadoEm();
}
