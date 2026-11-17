package dev.davidcaetano.umbra_api.monitoramento.mapper;

import dev.davidcaetano.umbra_api.catalogo.dto.response.OfertaResponse;
import dev.davidcaetano.umbra_api.monitoramento.dto.response.MonitoramentoResponse;
import dev.davidcaetano.umbra_api.monitoramento.repository.MonitoramentoResumoProjecao;
import org.mapstruct.Mapper;

@Mapper
public interface MonitoramentoMapper {

    MonitoramentoResponse paraResponse(MonitoramentoResumoProjecao resumo, OfertaResponse melhorPreco,
                                       Long distanciaDoAlvoCentavos, boolean alvoAtingido);

    OfertaResponse paraOferta(MonitoramentoResumoProjecao resumo, boolean gratuito, boolean promocaoEncerrada);
}
