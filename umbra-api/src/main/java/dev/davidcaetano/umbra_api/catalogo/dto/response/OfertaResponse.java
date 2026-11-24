package dev.davidcaetano.umbra_api.catalogo.dto.response;

import dev.davidcaetano.umbra_api.catalogo.enums.SituacaoDoPreco;
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
        SituacaoDoPreco situacao,
        Instant coletadoEm,
        Instant verificadoEm) {
}
