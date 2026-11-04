package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;

import java.time.Instant;

public record OfertaExterna(CodigoLoja codigoLoja,
                            String url,
                            long valorCentavos,
                            Long valorOriginalCentavos,
                            Short descontoPct,
                            Instant expiraEm) {
}
