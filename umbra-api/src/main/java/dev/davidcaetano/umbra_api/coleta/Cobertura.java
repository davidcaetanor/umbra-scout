package dev.davidcaetano.umbra_api.coleta;

/**
 * Cobertura que a fonte oferece numa rodada. Só CENSO autoriza concluir que uma oferta
 * saiu do ar (ARCHITECTURE.md §4.11).
 */
public enum Cobertura {
    AMOSTRA,
    CENSO
}
