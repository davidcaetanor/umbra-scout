package dev.davidcaetano.umbra_api.catalogo.dto.response;

import java.math.BigDecimal;

public record VariacaoResponse(Long precoAtualCentavos, Long diferencaCentavos, BigDecimal percentualAcimaDoMinimo) {
}
