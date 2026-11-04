package dev.davidcaetano.umbra_api.catalogo.dto.request;

import dev.davidcaetano.umbra_api.catalogo.enums.CategoriaHardware;
import dev.davidcaetano.umbra_api.catalogo.service.OrdenacaoCatalogo;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BuscaProdutoRequest(
        @Size(min = 3, max = 100, message = "deve ter entre 3 e 100 caracteres")
        @Pattern(regexp = "^[\\p{L}\\p{N} '\\-:.&+!]+$",
                message = "aceita só letras, dígitos, espaço e a pontuação ' - : . & + !")
        String nome,
        CategoriaHardware categoria,
        OrdenacaoCatalogo ordenacao,
        Boolean incluirExterno) {

    public BuscaProdutoRequest {
        nome = nome == null || nome.isBlank() ? null : nome.strip();
        incluirExterno = Boolean.TRUE.equals(incluirExterno);
    }

    @AssertTrue(message = "incluirExterno exige nome")
    public boolean isNomeInformadoParaBuscaExterna() {
        return !incluirExterno || nome != null;
    }
}
