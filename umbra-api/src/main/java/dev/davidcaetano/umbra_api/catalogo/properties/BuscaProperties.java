package dev.davidcaetano.umbra_api.catalogo.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "umbra.busca")
@Validated
public record BuscaProperties(@Valid @NotNull(message = "umbra.busca.externa é obrigatório") Externa externa) {

    public record Externa(
            @Min(value = 1, message = "umbra.busca.externa.teto-global deve ser maior ou igual a 1")
            int tetoGlobal,

            @Min(value = 1, message = "umbra.busca.externa.teto-por-ip deve ser maior ou igual a 1")
            int tetoPorIp,

            @NotNull(message = "umbra.busca.externa.janela é obrigatório")
            Duration janela,

            @NotNull(message = "umbra.busca.externa.timeout-conexao é obrigatório")
            Duration timeoutConexao,

            @NotNull(message = "umbra.busca.externa.timeout-leitura é obrigatório")
            Duration timeoutLeitura,

            @NotNull(message = "umbra.busca.externa.cache-ttl é obrigatório")
            Duration cacheTtl,

            @NotNull(message = "umbra.busca.externa.prazo é obrigatório")
            Duration prazo,

            @Min(value = 1, message = "umbra.busca.externa.maximo-resultados deve ser maior ou igual a 1")
            int maximoResultados) {
    }
}
