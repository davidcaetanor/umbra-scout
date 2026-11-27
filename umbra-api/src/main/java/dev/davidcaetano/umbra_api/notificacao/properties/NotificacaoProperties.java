package dev.davidcaetano.umbra_api.notificacao.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "umbra.notificacao")
@Validated
public record NotificacaoProperties(
        boolean avaliacaoHabilitada,

        @NotNull(message = "umbra.notificacao.teto-entre-avisos é obrigatório")
        Duration tetoEntreAvisos) {
}
