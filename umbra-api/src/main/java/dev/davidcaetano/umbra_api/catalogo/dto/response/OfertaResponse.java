package dev.davidcaetano.umbra_api.catalogo.dto.response;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record OfertaResponse(
        String loja,
        String url,
        long valorCentavos,
        @Nullable Long valorOriginalCentavos,
        @Nullable Short descontoPct,
        boolean gratuito,
        @Nullable Instant expiraEm,
        boolean promocaoEncerrada,
        Instant coletadoEm,
        Instant verificadoEm) {
}
