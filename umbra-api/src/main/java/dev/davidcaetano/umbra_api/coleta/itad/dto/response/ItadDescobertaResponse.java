package dev.davidcaetano.umbra_api.coleta.itad.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItadDescobertaResponse(
        int nextOffset,
        boolean hasMore,
        List<ItadJogoDescobertoResponse> list
) {
}
