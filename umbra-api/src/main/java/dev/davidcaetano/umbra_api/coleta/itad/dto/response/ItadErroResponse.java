package dev.davidcaetano.umbra_api.coleta.itad.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItadErroResponse(
        @JsonProperty("status_code") int statusCode,
        @JsonProperty("reason_phrase") String reasonPhrase
) {
}
