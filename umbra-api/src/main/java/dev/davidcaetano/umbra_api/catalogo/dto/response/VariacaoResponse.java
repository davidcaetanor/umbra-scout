package dev.davidcaetano.umbra_api.catalogo.dto.response;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

public record VariacaoResponse(@Nullable Long precoAtualCentavos,
                               @Nullable Long diferencaCentavos,
                               @Nullable BigDecimal percentualAcimaDoMinimo) {
}
