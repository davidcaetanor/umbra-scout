package dev.davidcaetano.umbra_api.notificacao.mapper;

import dev.davidcaetano.umbra_api.catalogo.service.ClassificacaoDePreco;
import dev.davidcaetano.umbra_api.notificacao.AlvoEmEnvio;
import dev.davidcaetano.umbra_api.notificacao.DestinatarioEmEnvio;
import dev.davidcaetano.umbra_api.notificacao.LojaEmEnvio;
import dev.davidcaetano.umbra_api.notificacao.MotivoDoAviso;
import dev.davidcaetano.umbra_api.notificacao.repository.AvisoPendenteProjecao;
import dev.davidcaetano.umbra_api.notificacao.repository.OfertaAtualProjecao;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DespachoDeAvisosMapper {

    public List<DestinatarioEmEnvio> agrupar(List<AvisoPendenteProjecao> pendentes, List<OfertaAtualProjecao> ofertas,
                                             Instant agora) {
        Map<Long, List<LojaEmEnvio>> lojasPorMonitoramento = ofertas.stream()
                .collect(Collectors.groupingBy(OfertaAtualProjecao::getMonitoramentoId,
                        Collectors.mapping(oferta -> paraLoja(oferta, agora), Collectors.toList())));

        Map<Long, List<AvisoPendenteProjecao>> porUsuario = new LinkedHashMap<>();
        pendentes.forEach(aviso -> porUsuario.computeIfAbsent(aviso.getUsuarioId(), id -> new ArrayList<>()).add(aviso));

        return porUsuario.values().stream()
                .map(avisos -> paraDestinatario(avisos, lojasPorMonitoramento))
                .toList();
    }

    private static DestinatarioEmEnvio paraDestinatario(List<AvisoPendenteProjecao> avisos,
                                                        Map<Long, List<LojaEmEnvio>> lojasPorMonitoramento) {
        Map<Long, List<AvisoPendenteProjecao>> porMonitoramento = new LinkedHashMap<>();
        avisos.forEach(aviso -> porMonitoramento.computeIfAbsent(aviso.getMonitoramentoId(), id -> new ArrayList<>())
                .add(aviso));

        List<AlvoEmEnvio> alvos = porMonitoramento.entrySet().stream()
                .map(grupo -> paraAlvo(grupo.getValue(), lojasPorMonitoramento.getOrDefault(grupo.getKey(), List.of())))
                .toList();
        AvisoPendenteProjecao primeiro = avisos.getFirst();
        return new DestinatarioEmEnvio(primeiro.getUsuarioId(), primeiro.getEmail(), alvos);
    }

    private static AlvoEmEnvio paraAlvo(List<AvisoPendenteProjecao> avisos, List<LojaEmEnvio> lojas) {
        AvisoPendenteProjecao ultimo = avisos.getLast();
        boolean baixouDeNovo = MotivoDoAviso.QUEDA.name().equals(ultimo.getMotivo());
        List<Long> avisoIds = avisos.stream().map(AvisoPendenteProjecao::getAvisoId).toList();
        return new AlvoEmEnvio(ultimo.getProdutoId(), ultimo.getNome(), ultimo.getPrecoAlvoCentavos(), baixouDeNovo,
                avisoIds, lojas);
    }

    private static LojaEmEnvio paraLoja(OfertaAtualProjecao oferta, Instant agora) {
        return new LojaEmEnvio(oferta.getLoja(), oferta.getValorCentavos(),
                ClassificacaoDePreco.situacao(oferta.getExpiraEm(), oferta.getVerificadoEm(), agora));
    }
}
