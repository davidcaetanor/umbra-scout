package dev.davidcaetano.umbra_api.catalogo.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "umbra.catalogo.qualidade")
@Validated
public record QualidadeProperties(
        @Min(value = 1, message = "umbra.catalogo.qualidade.lote deve ser maior ou igual a 1")
        int lote,

        @NotNull(message = "umbra.catalogo.qualidade.validade é obrigatório")
        Duration validade) {
}
