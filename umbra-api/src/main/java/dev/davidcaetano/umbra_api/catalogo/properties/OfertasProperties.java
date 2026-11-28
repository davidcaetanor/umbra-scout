package dev.davidcaetano.umbra_api.catalogo.properties;

import dev.davidcaetano.umbra_api.catalogo.repository.ReguaDeOfertas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
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
        Duration validadeDosDestaques,

        @NotNull(message = "umbra.ofertas.hardware é obrigatório")
        @Valid
        Hardware hardware,

        @NotNull(message = "umbra.ofertas.qualidade é obrigatório")
        @Valid
        Qualidade qualidade) {

    public record Qualidade(
            @Min(value = 1, message = "umbra.ofertas.qualidade.steam-avaliacoes-minimas deve ser maior ou igual a 1")
            int steamAvaliacoesMinimas,

            @Min(value = 0, message = "umbra.ofertas.qualidade.steam-nota-minima deve estar entre 0 e 100")
            @Max(value = 100, message = "umbra.ofertas.qualidade.steam-nota-minima deve estar entre 0 e 100")
            int steamNotaMinima,

            @Min(value = 0, message = "umbra.ofertas.qualidade.critica-nota-minima deve estar entre 0 e 100")
            @Max(value = 100, message = "umbra.ofertas.qualidade.critica-nota-minima deve estar entre 0 e 100")
            int criticaNotaMinima,

            @DecimalMin(value = "0.0", message = "umbra.ofertas.qualidade.media-do-catalogo deve estar entre 0 e 1")
            @DecimalMax(value = "1.0", message = "umbra.ofertas.qualidade.media-do-catalogo deve estar entre 0 e 1")
            double mediaDoCatalogo,

            @DecimalMin(value = "0.0", message = "umbra.ofertas.qualidade.piso deve estar entre 0 e 1")
            @DecimalMax(value = "1.0", message = "umbra.ofertas.qualidade.piso deve estar entre 0 e 1")
            double piso,

            @Min(value = 1, message = "umbra.ofertas.qualidade.destaques-por-publicador deve ser maior ou igual a 1")
            int destaquesPorPublicador) {
    }

    public record Hardware(
            @DecimalMin(value = "0.0", message = "umbra.ofertas.hardware.peso-economia deve estar entre 0 e 1")
            @DecimalMax(value = "1.0", message = "umbra.ofertas.hardware.peso-economia deve estar entre 0 e 1")
            double pesoEconomia,

            @DecimalMin(value = "0.0", message = "umbra.ofertas.hardware.peso-percentual deve estar entre 0 e 1")
            @DecimalMax(value = "1.0", message = "umbra.ofertas.hardware.peso-percentual deve estar entre 0 e 1")
            double pesoPercentual,

            @Min(value = 1, message = "umbra.ofertas.hardware.teto-economia-centavos deve ser maior ou igual a 1")
            long tetoEconomiaCentavos) {

        @AssertTrue(message = "umbra.ofertas.hardware: peso-economia e peso-percentual somam no máximo 1")
        public boolean isPesosDentroDoLimite() {
            return pesoEconomia + pesoPercentual <= 1.0;
        }
    }

    public ReguaDeOfertas regua() {
        return new ReguaDeOfertas(pesoEconomia, logaritmoDaReferencia(), hardware.pesoEconomia(),
                hardware.pesoPercentual(), hardware.tetoEconomiaCentavos(), qualidade.steamAvaliacoesMinimas(),
                qualidade.steamNotaMinima(), qualidade.criticaNotaMinima(), qualidade.mediaDoCatalogo(),
                qualidade.piso());
    }

    public double logaritmoDaReferencia() {
        return Math.log1p(referenciaEconomiaCentavos);
    }
}
