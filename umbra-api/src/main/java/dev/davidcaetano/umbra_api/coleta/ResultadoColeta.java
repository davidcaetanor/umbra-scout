package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;

import java.util.List;
import java.util.Objects;

public record ResultadoColeta(OrigemColeta fonte, List<OfertaColetada> ofertas, int totalElegivel, int totalSemPreco,
                               Reconciliacao reconciliacao) {

    public ResultadoColeta {
        Objects.requireNonNull(fonte, "fonte é obrigatória");
        Objects.requireNonNull(reconciliacao, "reconciliacao é obrigatória: fonte que não declara total usa declarado = null");
    }
}
