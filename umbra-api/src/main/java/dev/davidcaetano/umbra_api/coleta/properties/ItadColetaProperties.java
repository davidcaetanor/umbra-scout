package dev.davidcaetano.umbra_api.coleta.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "umbra.coleta.itad")
@Validated
public record ItadColetaProperties(
        @Min(value = 1, message = "umbra.coleta.itad.paginas-por-rodada deve ser maior ou igual a 1")
        int paginasPorRodada
) {
}
