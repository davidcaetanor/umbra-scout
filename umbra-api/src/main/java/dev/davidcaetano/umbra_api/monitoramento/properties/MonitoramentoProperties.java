package dev.davidcaetano.umbra_api.monitoramento.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "umbra.monitoramento")
@Validated
public record MonitoramentoProperties(
        @Min(value = 1, message = "umbra.monitoramento.dias-para-descontinuado deve ser maior ou igual a 1")
        int diasParaDescontinuado) {
}
