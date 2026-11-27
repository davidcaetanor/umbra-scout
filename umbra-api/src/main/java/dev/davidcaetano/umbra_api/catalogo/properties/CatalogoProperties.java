package dev.davidcaetano.umbra_api.catalogo.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "umbra.catalogo")
@Validated
public record CatalogoProperties(
        @Min(value = 1, message = "umbra.catalogo.prazo-da-consulta-segundos deve ser maior ou igual a 1")
        int prazoDaConsultaSegundos,

        @NotNull(message = "umbra.catalogo.tentar-de-novo-em é obrigatório")
        Duration tentarDeNovoEm) {
}
