package dev.davidcaetano.umbra_api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "umbra")
@Validated
public record UmbraProperties(@Valid @NotNull(message = "umbra.jwt é obrigatório") UmbraJWT jwt) {

    public record UmbraJWT(
            @NotBlank(message = "umbra.jwt.secret é obrigatório e não pode estar vazio!")
            String secret,

            @Min(value = 1, message = "umbra.jwt.ttl-horas deve ser maior ou igual a 1")
            int ttlHoras
    ) {
    }

}
