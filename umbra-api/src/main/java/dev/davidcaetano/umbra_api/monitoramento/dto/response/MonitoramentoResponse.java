package dev.davidcaetano.umbra_api.monitoramento.dto.response;

import dev.davidcaetano.umbra_api.catalogo.dto.response.OfertaResponse;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record MonitoramentoResponse(
        Long id,
        Long produtoId,
        String nome,
        @Nullable String imagemUrl,
        long precoAlvoCentavos,
        boolean ativo,
        Instant criadoEm,
        @Nullable OfertaResponse melhorPreco,
        @Nullable Long distanciaDoAlvoCentavos,
        boolean alvoAtingido,
        boolean aparentementeDescontinuado) {
}
