package dev.davidcaetano.umbra_api.coleta.itad.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItadOfertaPrecoResponse(
        ItadLojaResponse shop,
        ItadValorResponse price,
        ItadValorResponse regular,
        int cut,
        OffsetDateTime timestamp,
        OffsetDateTime expiry,
        String url
) {
}
