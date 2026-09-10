package dev.davidcaetano.umbra_api.coleta.kabum.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KabumProdutoResponse(
        String type,
        long id,
        KabumAtributosResponse attributes
) {
}
