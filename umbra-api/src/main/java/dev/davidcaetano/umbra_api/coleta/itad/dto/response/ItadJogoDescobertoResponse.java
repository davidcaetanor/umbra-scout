package dev.davidcaetano.umbra_api.coleta.itad.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItadJogoDescobertoResponse(
        UUID id,
        String title,
        String type,
        ItadImagemResponse assets,
        ItadOfertaDescobertaResponse deal
) {
}
