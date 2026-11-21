package dev.davidcaetano.umbra_api.monitoramento.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

public record FiltroDeMonitoramentosRequest(
        @Schema(description = "Filtra pelo produto do catálogo.")
        @Positive(message = "deve ser maior que 0")
        Long produtoId) {
}
