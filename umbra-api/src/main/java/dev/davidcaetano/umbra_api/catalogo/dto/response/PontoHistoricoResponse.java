package dev.davidcaetano.umbra_api.catalogo.dto.response;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record PontoHistoricoResponse(Instant instante, @Nullable Long valorCentavos) {
}
