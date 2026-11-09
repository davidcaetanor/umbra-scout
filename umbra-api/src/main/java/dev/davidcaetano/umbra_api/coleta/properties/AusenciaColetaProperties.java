package dev.davidcaetano.umbra_api.coleta.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "umbra.coleta.ausencia")
@Validated
public record AusenciaColetaProperties(
        @Min(value = 1, message = "umbra.coleta.ausencia.teto-percentual deve ser maior ou igual a 1")
        @Max(value = 100, message = "umbra.coleta.ausencia.teto-percentual deve ser menor ou igual a 100")
        int tetoPercentual
) {
}
