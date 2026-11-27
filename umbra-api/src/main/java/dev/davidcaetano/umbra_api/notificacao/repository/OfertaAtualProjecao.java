package dev.davidcaetano.umbra_api.notificacao.repository;

import java.time.Instant;

public interface OfertaAtualProjecao {

    Long getMonitoramentoId();

    String getLoja();

    long getValorCentavos();

    Instant getExpiraEm();

    Instant getVerificadoEm();
}
