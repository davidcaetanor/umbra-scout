package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;

import java.util.Objects;
import java.util.Set;

/**
 * O que a rodada pretendeu cobrir. Só loja coberta como censo autoriza concluir que uma oferta
 * saiu do ar (ARCHITECTURE.md §4.11).
 */
public record Cobertura(Set<CodigoLoja> lojasEmCenso) {

    public Cobertura {
        Objects.requireNonNull(lojasEmCenso, "lojasEmCenso é obrigatório: amostra usa Cobertura.amostra()");
        lojasEmCenso = Set.copyOf(lojasEmCenso);
    }

    public static Cobertura amostra() {
        return new Cobertura(Set.of());
    }

    public static Cobertura censoDe(CodigoLoja loja) {
        return new Cobertura(Set.of(loja));
    }

    public boolean ehCenso() {
        return !lojasEmCenso.isEmpty();
    }
}
