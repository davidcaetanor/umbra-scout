package dev.davidcaetano.umbra_api.coleta.steam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamCategoriaResponse(
        int id,
        String name,
        List<SteamJogoDescobertoResponse> items
) {
}
