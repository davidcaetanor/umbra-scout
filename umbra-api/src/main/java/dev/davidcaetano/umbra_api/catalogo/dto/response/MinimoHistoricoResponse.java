package dev.davidcaetano.umbra_api.catalogo.dto.response;

import java.time.Instant;

public record MinimoHistoricoResponse(long valorCentavos, boolean gratuito, String loja, Instant em) {
}
