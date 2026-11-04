package dev.davidcaetano.umbra_api.catalogo.dto.response;

import java.time.Instant;

public record OfertaResponse(
        String loja,
        String url,
        long valorCentavos,
        Long valorOriginalCentavos,
        Short descontoPct,
        boolean gratuito,
        Instant expiraEm,
        boolean promocaoEncerrada,
        Instant coletadoEm) {
}
