package dev.davidcaetano.umbra_api.catalogo.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.davidcaetano.umbra_api.catalogo.enums.CategoriaHardware;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.catalogo.service.OrdenacaoCatalogo;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BuscaProdutoRequest(
        @Size(min = 3, max = 100, message = "deve ter entre 3 e 100 caracteres")
        @Pattern(regexp = "^[\\p{L}\\p{N} '\\-:.&+!]+$",
                message = "aceita só letras, dígitos, espaço e a pontuação ' - : . & + !")
        String nome,
        TipoProduto tipo,
        CategoriaHardware categoria,
        OrdenacaoCatalogo ordenacao,
        Boolean incluirExterno) {

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
    @AssertTrue(message = "categoria só se aplica a HARDWARE")
    public boolean isCategoriaCompativelComTipo() {
        return categoria == null || tipo != TipoProduto.JOGO;
    }
}
