package dev.davidcaetano.umbra_api.coleta.kabum.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KabumCampanhaResponse(
        String name,

        @JsonProperty("starts_at")
        Long startsAt,

        @JsonProperty("ends_at")
        Long endsAt,

        @JsonProperty("quantity_available")
        Integer quantityAvailable,

        @JsonProperty("price_with_discount")
        BigDecimal priceWithDiscount,

        @JsonProperty("discount_percentage")
        int discountPercentage,

        @JsonProperty("is_logged_user_exclusive")
        boolean isLoggedUserExclusive
) {
}
