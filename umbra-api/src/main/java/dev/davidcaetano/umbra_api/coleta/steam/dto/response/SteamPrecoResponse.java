package dev.davidcaetano.umbra_api.coleta.steam.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamPrecoResponse(
        String currency,
        int initial,
        @JsonProperty("final") int finalPrice,
        @JsonProperty("discount_percent") int discountPercent
) {
}
