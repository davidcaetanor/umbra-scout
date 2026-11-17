package dev.davidcaetano.umbra_api.monitoramento.repository;

import java.time.Instant;

public interface MonitoramentoResumoProjecao {

    Long getId();

    Long getProdutoId();

    String getNome();

    String getImagemUrl();

    long getPrecoAlvoCentavos();

    boolean isAtivo();

    Instant getCriadoEm();

    boolean isAparentementeDescontinuado();

    String getLoja();

    String getUrl();

    Long getValorCentavos();

    Long getValorOriginalCentavos();

    Short getDescontoPct();

    Instant getExpiraEm();

    Instant getColetadoEm();
}
