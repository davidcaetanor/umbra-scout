package dev.davidcaetano.umbra_api.monitoramento.dto.response;

import dev.davidcaetano.umbra_api.catalogo.dto.response.OfertaResponse;

import java.time.Instant;

public record MonitoramentoResponse(
        Long id,
        Long produtoId,
        String nome,
        String imagemUrl,
        long precoAlvoCentavos,
        boolean ativo,
        Instant criadoEm,
        OfertaResponse melhorPreco,
        Long distanciaDoAlvoCentavos,
        boolean alvoAtingido,
        boolean aparentementeDescontinuado) {
}
