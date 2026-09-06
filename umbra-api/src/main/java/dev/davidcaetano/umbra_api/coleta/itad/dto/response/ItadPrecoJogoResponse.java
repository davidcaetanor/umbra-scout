package dev.davidcaetano.umbra_api.coleta.itad.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItadPrecoJogoResponse(
        UUID id,
        List<ItadOfertaPrecoResponse> deals
) {
}
