package dev.davidcaetano.umbra_api.catalogo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

public final class IndicadoresDePreco {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final int ESCALA_DO_PERCENTUAL = 1;
    private static final int QUANTIDADE_DE_FAIXAS = 4;

    private IndicadoresDePreco() {
    }

    public static VariacaoDePreco variacao(Long precoAtual, Long minimo) {
        if (precoAtual == null || minimo == null) {
            return new VariacaoDePreco(precoAtual, null, null);
        }

        long diferenca = precoAtual - minimo;
        return new VariacaoDePreco(precoAtual, diferenca, percentualAcimaDoMinimo(diferenca, minimo));
    }

    public static List<Long> faixas(List<Long> valoresDoPeriodo) {
        if (valoresDoPeriodo.isEmpty()) {
            return List.of();
        }

        long menor = Collections.min(valoresDoPeriodo);
        long maior = Collections.max(valoresDoPeriodo);
        long passo = (maior - menor) / QUANTIDADE_DE_FAIXAS;

        return List.of(menor, menor + passo, menor + 2 * passo, menor + 3 * passo, maior);
    }

    private static BigDecimal percentualAcimaDoMinimo(long diferenca, long minimo) {
        if (minimo == 0) {
            return diferenca == 0 ? BigDecimal.ZERO.setScale(ESCALA_DO_PERCENTUAL) : null;
        }

        return BigDecimal.valueOf(diferenca)
                .multiply(CEM)
                .divide(BigDecimal.valueOf(minimo), ESCALA_DO_PERCENTUAL, RoundingMode.HALF_UP);
    }
}
