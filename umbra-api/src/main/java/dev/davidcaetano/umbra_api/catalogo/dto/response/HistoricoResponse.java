package dev.davidcaetano.umbra_api.catalogo.dto.response;

import java.time.Instant;
import java.util.List;

public record HistoricoResponse(
        String periodo,
        Instant consultadoEm,
        Instant observadoDesde,
        MinimoHistoricoResponse minimo,
        VariacaoResponse variacao,
        List<PontoHistoricoResponse> melhorPreco,
        List<SerieLojaResponse> porLoja,
        List<Long> faixas) {
}
