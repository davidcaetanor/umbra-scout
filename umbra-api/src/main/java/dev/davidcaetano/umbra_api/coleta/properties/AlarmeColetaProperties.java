package dev.davidcaetano.umbra_api.coleta.properties;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@ConfigurationProperties(prefix = "umbra.coleta.alarme")
@Validated
public record AlarmeColetaProperties(
        @Valid
        @NotNull(message = "umbra.coleta.alarme.padrao e obrigatorio")
        Limiares padrao,

        Map<OrigemColeta, @Valid Sobrescrita> porFonte
) {

    public AlarmeColetaProperties {
        porFonte = porFonte == null ? Map.of() : Map.copyOf(porFonte);
    }

    public Limiares limiaresDe(OrigemColeta fonte) {

        Sobrescrita sobrescrita = porFonte.get(fonte);
        if (sobrescrita == null) {
            return padrao;
        }

        return new Limiares(
                valorOuPadrao(sobrescrita.amostraMinima(), padrao.amostraMinima()),
                valorOuPadrao(sobrescrita.limiarSemPrecoPercentual(), padrao.limiarSemPrecoPercentual()),
                valorOuPadrao(sobrescrita.margemCoberturaPercentual(), padrao.margemCoberturaPercentual()),
                valorOuPadrao(sobrescrita.margemDuplicataPercentual(), padrao.margemDuplicataPercentual()));
    }

    private static int valorOuPadrao(Integer valor, int padrao) {
        return valor != null ? valor : padrao;
    }

    public record Limiares(
            @Min(value = 1, message = "umbra.coleta.alarme.padrao.amostra-minima deve ser maior ou igual a 1")
            int amostraMinima,

            @Min(value = 0, message = "umbra.coleta.alarme.padrao.limiar-sem-preco-percentual deve ser maior ou igual a 0")
            @Max(value = 100, message = "umbra.coleta.alarme.padrao.limiar-sem-preco-percentual deve ser menor ou igual a 100")
            int limiarSemPrecoPercentual,

            @Min(value = 0, message = "umbra.coleta.alarme.padrao.margem-cobertura-percentual deve ser maior ou igual a 0")
            @Max(value = 100, message = "umbra.coleta.alarme.padrao.margem-cobertura-percentual deve ser menor ou igual a 100")
            int margemCoberturaPercentual,

            @Min(value = 0, message = "umbra.coleta.alarme.padrao.margem-duplicata-percentual deve ser maior ou igual a 0")
            @Max(value = 100, message = "umbra.coleta.alarme.padrao.margem-duplicata-percentual deve ser menor ou igual a 100")
            int margemDuplicataPercentual
    ) {
    }

    public record Sobrescrita(
            @Min(value = 1, message = "umbra.coleta.alarme.por-fonte.<fonte>.amostra-minima deve ser maior ou igual a 1")
            Integer amostraMinima,

            @Min(value = 0, message = "umbra.coleta.alarme.por-fonte.<fonte>.limiar-sem-preco-percentual deve ser maior ou igual a 0")
            @Max(value = 100, message = "umbra.coleta.alarme.por-fonte.<fonte>.limiar-sem-preco-percentual deve ser menor ou igual a 100")
            Integer limiarSemPrecoPercentual,

            @Min(value = 0, message = "umbra.coleta.alarme.por-fonte.<fonte>.margem-cobertura-percentual deve ser maior ou igual a 0")
            @Max(value = 100, message = "umbra.coleta.alarme.por-fonte.<fonte>.margem-cobertura-percentual deve ser menor ou igual a 100")
            Integer margemCoberturaPercentual,

            @Min(value = 0, message = "umbra.coleta.alarme.por-fonte.<fonte>.margem-duplicata-percentual deve ser maior ou igual a 0")
            @Max(value = 100, message = "umbra.coleta.alarme.por-fonte.<fonte>.margem-duplicata-percentual deve ser menor ou igual a 100")
            Integer margemDuplicataPercentual
    ) {
    }
}
