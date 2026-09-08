package dev.davidcaetano.umbra_api.coleta.steam.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamCategoriaResponse(
        String id,
        String name,
        List<SteamJogoDescobertoResponse> items
) {
}
