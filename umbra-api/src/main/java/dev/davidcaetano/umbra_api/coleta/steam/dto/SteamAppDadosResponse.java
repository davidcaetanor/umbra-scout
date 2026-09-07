package dev.davidcaetano.umbra_api.coleta.steam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamAppDadosResponse(
        String type,
        @JsonProperty("price_overview") SteamPrecoResponse overview
) {
}
