package dev.davidcaetano.umbra_api.monitoramento.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "umbra.monitoramento")
@Validated
public record MonitoramentoProperties(
        @Min(value = 1, message = "umbra.monitoramento.dias-para-descontinuado deve ser maior ou igual a 1")
        int diasParaDescontinuado,

        @Valid @NotNull(message = "umbra.monitoramento.externo é obrigatório")
        Externo externo) {

    public record Externo(
            @NotNull(message = "umbra.monitoramento.externo.prazo é obrigatório")
            Duration prazo,

            @NotNull(message = "umbra.monitoramento.externo.tentar-de-novo-em é obrigatório")
            Duration tentarDeNovoEm) {
    }
}
