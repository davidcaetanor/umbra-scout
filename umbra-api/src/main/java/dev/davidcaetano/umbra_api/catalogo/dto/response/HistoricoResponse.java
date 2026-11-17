package dev.davidcaetano.umbra_api.catalogo.dto.response;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

public record HistoricoResponse(
        String periodo,
        Instant consultadoEm,
        @Nullable Instant observadoDesde,
        @Nullable MinimoHistoricoResponse minimo,
        VariacaoResponse variacao,
        List<PontoHistoricoResponse> melhorPreco,
        List<SerieLojaResponse> porLoja,
        List<Long> faixas) {
}
