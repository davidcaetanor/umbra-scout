package dev.davidcaetano.umbra_api.notificacao;

import java.util.List;
import java.util.Objects;

public record MonitoramentoEmAvaliacao(Long monitoramentoId, EstadoDoAviso estado, List<OfertaAvaliada> ofertas) {

    public MonitoramentoEmAvaliacao {
        Objects.requireNonNull(monitoramentoId, "monitoramentoId é obrigatório");
        Objects.requireNonNull(estado, "estado é obrigatório");
        ofertas = List.copyOf(ofertas);
    }
}
