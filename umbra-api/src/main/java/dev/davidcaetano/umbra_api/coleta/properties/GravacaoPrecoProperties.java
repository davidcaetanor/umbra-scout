package dev.davidcaetano.umbra_api.coleta.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "umbra.coleta.gravacao")
@Validated
public record GravacaoPrecoProperties(
        @Min(value = 1, message = "umbra.coleta.gravacao.janela-vivacidade-dias deve ser maior ou igual a 1")
        int janelaVivacidadeDias
) {
}
