package dev.davidcaetano.umbra_api.coleta.kabum.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KabumPrimeResponse(
        BigDecimal price,

        @JsonProperty("price_with_discount")
        BigDecimal priceWithDiscount,

        @JsonProperty("is_logged_user_exclusive")
        boolean isLoggedUserExclusive
) {
}
