package dev.davidcaetano.umbra_api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties(prefix = "umbra")
@Validated
public record UmbraProperties(
        @Valid @NotNull(message = "umbra.jwt é obrigatório") UmbraJWT jwt,
        @Valid @NotNull(message = "umbra.coleta e obrigatorio (umbra.coleta.contato)") UmbraColeta coleta,
        @Valid @DefaultValue UmbraCors cors
) {

    public record UmbraJWT(
            @NotBlank(message = "umbra.jwt.secret é obrigatório e não pode estar vazio!")
            String secret,

            @Min(value = 1, message = "umbra.jwt.ttl-horas deve ser maior ou igual a 1")
            int ttlHoras
    ) {

        private static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

        public UmbraJWT {
            if (secret != null && !secret.isBlank() && secret.length() < TAMANHO_MINIMO_DO_SEGREDO) {
                throw new IllegalArgumentException("umbra.jwt.secret (UMBRA_JWT_SECRET) deve ter pelo menos "
                        + TAMANHO_MINIMO_DO_SEGREDO + " caracteres (256 bits); o valor recusado não é exibido");
            }
        }

        @Override
        public String toString() {
            return "UmbraJWT[secret=***, ttlHoras=" + ttlHoras + "]";
        }
    }

    public record UmbraCors(
            @DefaultValue
            List<@Pattern(regexp = "https?://[A-Za-z0-9.-]+(:[0-9]{1,5})?",
                    message = "umbra.cors.origens aceita só esquema, host e porta, sem curinga nem caminho")
                    String> origens
    ) {
    }

    public record UmbraColeta(
            @NotBlank(message = "umbra.coleta.contato e obrigatorio e nao pode estar vazio!")
            @Email(message = "umbra.coleta.contato deve ser um email valido")
            String contato
    ) {
    }

}
