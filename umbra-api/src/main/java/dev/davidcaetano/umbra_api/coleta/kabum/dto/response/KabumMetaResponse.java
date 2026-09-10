package dev.davidcaetano.umbra_api.coleta.kabum.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KabumMetaResponse(
        @JsonProperty("total_items_count")
        long totalItemsCount,

        @JsonProperty("total_pages_count")
        int totalPagesCount,

        KabumPaginaResponse page
) {
}
