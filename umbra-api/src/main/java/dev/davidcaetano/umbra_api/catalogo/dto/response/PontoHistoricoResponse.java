package dev.davidcaetano.umbra_api.catalogo.dto.response;

import java.time.Instant;

public record PontoHistoricoResponse(Instant instante, Long valorCentavos) {
}
