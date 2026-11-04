package dev.davidcaetano.umbra_api.catalogo.dto.response;

import java.util.List;

public record SerieLojaResponse(String loja, List<PontoHistoricoResponse> pontos) {
}
