package dev.davidcaetano.umbra_api.usuario.viacep.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "viacep")
@Validated
public record ViaCepProperties(
        @NotBlank(message = "viacep.base-url é obrigatório e não pode estar vazio!")
        String baseUrl,

        @NotNull(message = "viacep.timeout-conexao é obrigatório")
        Duration timeoutConexao,

        @NotNull(message = "viacep.timeout-leitura é obrigatório")
        Duration timeoutLeitura) {
}
