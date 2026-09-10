package dev.davidcaetano.umbra_api.coleta.kabum.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "kabum")
@Validated
public record KabumProperties(@Valid @NotNull(message = "kabum.kabum-api é obrigatório") KabumApi kabumApi) {

    public record KabumApi(
            @NotBlank(message = "kabum.kabum-api.base-url é obrigatório e não pode estar vazio!") String baseUrl) {
    }
}
