package dev.davidcaetano.umbra_api.coleta.itad.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItadLojaResponse(
        int id,
        String name
) {
}
