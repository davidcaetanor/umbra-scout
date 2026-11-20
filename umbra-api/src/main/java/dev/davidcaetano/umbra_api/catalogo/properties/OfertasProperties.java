package dev.davidcaetano.umbra_api.catalogo.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "umbra.ofertas")
@Validated
public record OfertasProperties(
        @DecimalMin(value = "0.0", message = "umbra.ofertas.peso-economia deve estar entre 0 e 1")
        @DecimalMax(value = "1.0", message = "umbra.ofertas.peso-economia deve estar entre 0 e 1")
        double pesoEconomia,

        @Min(value = 1, message = "umbra.ofertas.referencia-economia-centavos deve ser maior ou igual a 1")
        long referenciaEconomiaCentavos,

        @Min(value = 1, message = "umbra.ofertas.destaques deve ser maior ou igual a 1")
        int destaques,

        @Min(value = 1, message = "umbra.ofertas.candidatos-dos-destaques deve ser maior ou igual a 1")
        int candidatosDosDestaques,

        @NotNull(message = "umbra.ofertas.validade-dos-destaques é obrigatório")
        Duration validadeDosDestaques) {

    public double pesoMinimo() {
        return 1.0 - pesoEconomia;
    }

    public double logaritmoDaReferencia() {
        return Math.log1p(referenciaEconomiaCentavos);
    }
}
