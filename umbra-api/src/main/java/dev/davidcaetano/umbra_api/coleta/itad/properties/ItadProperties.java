package dev.davidcaetano.umbra_api.coleta.itad.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "itad")
@Validated
public record ItadProperties(@Valid @NotNull(message = "itad.itad-api é obrigatório") ItadApi itadApi) {

    public record ItadApi(
            @NotBlank(message = "itad.itad-api.api-key é obrigatório e não pode estar vazio!")
            String apiKey,

            @NotBlank(message = "itad.itad-api.base-url é obrigatório e não pode estar vazio!")
            String baseUrl) {
    }
}
