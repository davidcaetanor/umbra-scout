package dev.davidcaetano.umbra_api.coleta.kabum.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KabumAtributosResponse(
        String menu,
        String title,
        BigDecimal price,

        @JsonProperty("price_with_discount")
        BigDecimal priceWithDiscount,

        boolean available,
        int stock,

        @JsonProperty("is_marketplace")
        boolean isMarketplace,

        @JsonProperty("product_link")
        String productLink,

        KabumPrimeResponse prime
) {
}
