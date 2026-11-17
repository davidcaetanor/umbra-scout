package dev.davidcaetano.umbra_api.monitoramento.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AlterarMonitoramentoRequest(
        Boolean ativo,
        @Min(value = PrecoAlvo.MINIMO_CENTAVOS, message = "deve ser maior ou igual a 1")
        @Max(value = PrecoAlvo.MAXIMO_CENTAVOS, message = "deve ser menor ou igual a 100000000")
        Long precoAlvoCentavos) {

    @AssertTrue(message = "informe ativo ou precoAlvoCentavos")
    public boolean isAlgumCampoInformado() {
        return ativo != null || precoAlvoCentavos != null;
    }
}
