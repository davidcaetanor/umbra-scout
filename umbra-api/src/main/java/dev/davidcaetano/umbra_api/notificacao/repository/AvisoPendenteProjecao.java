package dev.davidcaetano.umbra_api.notificacao.repository;

public interface AvisoPendenteProjecao {

    Long getAvisoId();

    String getMotivo();

    Long getMonitoramentoId();

    Long getUsuarioId();

    String getEmail();

    long getPrecoAlvoCentavos();

    Long getProdutoId();

    String getNome();
}
