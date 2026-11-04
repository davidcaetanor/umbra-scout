package dev.davidcaetano.umbra_api.catalogo.service;

import java.math.BigDecimal;

public record VariacaoDePreco(Long precoAtualCentavos, Long diferencaCentavos, BigDecimal percentualAcimaDoMinimo) {
}
