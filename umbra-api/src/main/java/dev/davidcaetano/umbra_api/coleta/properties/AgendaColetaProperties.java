package dev.davidcaetano.umbra_api.coleta.properties;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@ConfigurationProperties(prefix = "umbra.coleta.agenda")
@Validated
public record AgendaColetaProperties(
        @NotEmpty(message = "umbra.coleta.agenda.descoberta e obrigatorio e nao pode estar vazio!")
        Map<OrigemColeta, String> descoberta,

        @NotBlank(message = "umbra.coleta.agenda.refresh e obrigatorio e nao pode estar vazio!")
        String refresh,

        boolean rodadaInicial
) {
}
