package dev.davidcaetano.umbra_api.coleta.itad.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItadJogoInfoResponse(
        UUID id,
        String title,
        String type,
        ItadImagemResponse assets,
        List<ItadAvaliacaoResponse> reviews,
        ItadEstatisticasResponse stats,
        List<ItadEmpresaResponse> publishers
) {
}
