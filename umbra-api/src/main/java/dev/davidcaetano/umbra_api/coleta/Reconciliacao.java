package dev.davidcaetano.umbra_api.coleta;

/**
 * Medida de cobertura de uma rodada. declarado é nulo quando a fonte não declara total;
 * brutos e distintos contam a unidade paginada, antes de qualquer filtro de escopo nosso.
 */
public record Reconciliacao(Long declarado, int brutos, int distintos) {
}
