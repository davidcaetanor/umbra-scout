package dev.davidcaetano.umbra_api.notificacao.properties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "umbra.notificacao.envio")
@Validated
public record EnvioDeAvisosProperties(
        @NotBlank(message = "umbra.notificacao.envio.remetente é obrigatório com o envio ligado")
        @Email(message = "umbra.notificacao.envio.remetente deve ser um email")
        String remetente,

        @NotBlank(message = "umbra.notificacao.envio.url-do-front é obrigatório com o envio ligado")
        @Pattern(regexp = "^https?://[A-Za-z0-9.-]+(:[0-9]+)?$",
                message = "umbra.notificacao.envio.url-do-front deve ser só a origem, sem caminho nem barra no fim")
        String urlDoFront,

        @Positive(message = "umbra.notificacao.envio.teto-diario-por-usuario deve ser positivo")
        int tetoDiarioPorUsuario,

        @Positive(message = "umbra.notificacao.envio.itens-por-email deve ser positivo")
        int itensPorEmail,

        @Positive(message = "umbra.notificacao.envio.tentativas deve ser positivo")
        int tentativas,

        @NotNull(message = "umbra.notificacao.envio.validade-do-pendente é obrigatório")
        Duration validadeDoPendente) {
}
