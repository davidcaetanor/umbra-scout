package dev.davidcaetano.umbra_api.monitoramento.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CriarMonitoramentoRequest(
        @NotNull(message = "é obrigatório")
        @Positive(message = "deve ser maior que zero")
        Long produtoId,
        @NotNull(message = "é obrigatório")
        @Min(value = PrecoAlvo.MINIMO_CENTAVOS, message = "deve ser maior ou igual a 1")
        @Max(value = PrecoAlvo.MAXIMO_CENTAVOS, message = "deve ser menor ou igual a 100000000")
        Long precoAlvoCentavos) {
}
