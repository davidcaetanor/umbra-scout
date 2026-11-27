package dev.davidcaetano.umbra_api.notificacao.mapper;

import dev.davidcaetano.umbra_api.catalogo.service.ClassificacaoDePreco;
import dev.davidcaetano.umbra_api.monitoramento.repository.OfertaEmAvaliacaoProjecao;
import dev.davidcaetano.umbra_api.notificacao.EstadoDoAviso;
import dev.davidcaetano.umbra_api.notificacao.MonitoramentoEmAvaliacao;
import dev.davidcaetano.umbra_api.notificacao.OfertaAvaliada;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AvaliacaoDeAlvoMapper {

    public List<MonitoramentoEmAvaliacao> agrupar(List<OfertaEmAvaliacaoProjecao> linhas, Instant agora) {
        Map<Long, List<OfertaEmAvaliacaoProjecao>> porMonitoramento = new LinkedHashMap<>();
        linhas.forEach(linha -> porMonitoramento
                .computeIfAbsent(linha.getMonitoramentoId(), id -> new ArrayList<>())
                .add(linha));

        return porMonitoramento.entrySet().stream()
                .map(grupo -> paraMonitoramento(grupo.getKey(), grupo.getValue(), agora))
                .toList();
    }

    private static MonitoramentoEmAvaliacao paraMonitoramento(Long monitoramentoId,
                                                              List<OfertaEmAvaliacaoProjecao> linhas, Instant agora) {
        OfertaEmAvaliacaoProjecao primeira = linhas.getFirst();
        EstadoDoAviso estado = new EstadoDoAviso(primeira.getPrecoAlvoCentavos(), primeira.getPrecoAvisadoCentavos(),
                primeira.getUltimoAvisoEm(), primeira.isForaDeVenda());
        List<OfertaAvaliada> ofertas = linhas.stream()
                .map(linha -> new OfertaAvaliada(linha.getOfertaId(), linha.getValorCentavos(),
                        ClassificacaoDePreco.situacao(linha.getExpiraEm(), linha.getVerificadoEm(), agora)))
                .toList();
        return new MonitoramentoEmAvaliacao(monitoramentoId, estado, ofertas);
    }
}
