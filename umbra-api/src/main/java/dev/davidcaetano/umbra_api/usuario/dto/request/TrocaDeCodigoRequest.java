package dev.davidcaetano.umbra_api.usuario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocaDeCodigoRequest(
        @NotBlank(message = "codigo é obrigatório")
        @Size(max = 64, message = "codigo deve ter no máximo 64 caracteres")
        String codigo) {

    @Override
    public String toString() {
        return "TrocaDeCodigoRequest[codigo=***]";
    }
}
