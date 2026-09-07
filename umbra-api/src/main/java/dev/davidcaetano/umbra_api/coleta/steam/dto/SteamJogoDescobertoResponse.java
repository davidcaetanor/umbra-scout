package dev.davidcaetano.umbra_api.coleta.steam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamJogoDescobertoResponse(
        int id,
        String name,
        boolean discounted,
        @JsonProperty("discount_expiration") long discountExpiration,
        @JsonProperty("header_image") String headerImage
) {
}
