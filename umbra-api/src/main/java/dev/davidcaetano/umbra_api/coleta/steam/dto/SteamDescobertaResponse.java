package dev.davidcaetano.umbra_api.coleta.steam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamDescobertaResponse(SteamCategoriaResponse specials) {
}
