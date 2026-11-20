package dev.davidcaetano.umbra_api.catalogo.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.davidcaetano.umbra_api.catalogo.enums.CategoriaHardware;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.catalogo.service.OrdenacaoCatalogo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record BuscaProdutoRequest(
        @Size(min = 3, max = 100, message = "deve ter entre 3 e 100 caracteres")
        @Pattern(regexp = "^[\\p{L}\\p{N} '\\-:.&+!]+$",
                message = "aceita só letras, dígitos, espaço e a pontuação ' - : . & + !")
        String nome,
        TipoProduto tipo,
        CategoriaHardware categoria,
        OrdenacaoCatalogo ordenacao,
        Boolean incluirExterno,
        @Schema(description = "Preço mínimo do melhor preço atual, em centavos.")
        @PositiveOrZero(message = "deve ser maior ou igual a 0")
        Long precoMin,
        @Schema(description = "Preço máximo do melhor preço atual, em centavos.")
        @PositiveOrZero(message = "deve ser maior ou igual a 0")
        Long precoMax,
        @Schema(description = "Desconto mínimo em percentual. Só com tipo=JOGO.")
        @Min(value = 1, message = "deve estar entre 1 e 100")
        @Max(value = 100, message = "deve estar entre 1 e 100")
        Integer descontoMin,
        @Schema(description = "Loja. Com ela, preço e ordem passam a ser os dessa loja.")
        CodigoLoja loja) {

    public BuscaProdutoRequest {
        nome = nome == null || nome.isBlank() ? null : nome.strip();
        incluirExterno = Boolean.TRUE.equals(incluirExterno);
    }

    @JsonIgnore
    @AssertTrue(message = "incluirExterno exige nome")
    public boolean isNomeInformadoParaBuscaExterna() {
        return !incluirExterno || nome != null;
    }

    @JsonIgnore
    @AssertTrue(message = "precoMin não pode passar de precoMax")
    public boolean isFaixaDePrecoValida() {
        return precoMin == null || precoMax == null || precoMin <= precoMax;
    }

    @JsonIgnore
    @AssertTrue(message = "melhoresOfertas, maiorDesconto e descontoMin exigem tipo=JOGO")
    public boolean isCriterioDeOfertaComTipoJogo() {
        boolean pedeCriterioDeOferta = descontoMin != null || ordenacao != null && ordenacao.exclusivaDeJogo();
        return !pedeCriterioDeOferta || tipo == TipoProduto.JOGO;
    }

    @JsonIgnore
    @AssertTrue(message = "categoria só se aplica a HARDWARE")
    public boolean isCategoriaCompativelComTipo() {
        return categoria == null || tipo != TipoProduto.JOGO;
    }
}
