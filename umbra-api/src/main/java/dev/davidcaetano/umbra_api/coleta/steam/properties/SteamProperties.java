package dev.davidcaetano.umbra_api.coleta.steam.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "steam")
@Validated
public record SteamProperties(@Valid @NotNull(message = "steam.steam-api é obrigatório") SteamApi steamApi) {

    public record SteamApi(
            @NotBlank(message = "steam.steam-api.base-url é obrigatório e não pode estar vazio!") String baseUrl) {
    }
}
