package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;

import java.util.Objects;
import java.util.Set;


public record Cobertura(Set<CodigoLoja> lojasEmCenso, Set<String> chavesItadEmCenso) {

    public Cobertura {
        Objects.requireNonNull(lojasEmCenso, "lojasEmCenso e obrigatorio: amostra usa Cobertura.amostra()");
        Objects.requireNonNull(chavesItadEmCenso, "chavesItadEmCenso e obrigatorio: censo da loja inteira usa Cobertura.censoDe(loja)");
        lojasEmCenso = Set.copyOf(lojasEmCenso);
        chavesItadEmCenso = Set.copyOf(chavesItadEmCenso);
    }

    public static Cobertura amostra() {
        return new Cobertura(Set.of(), Set.of());
    }

    public static Cobertura censoDe(CodigoLoja loja) {
        return new Cobertura(Set.of(loja), Set.of());
    }

    public static Cobertura censoDosProdutos(Set<CodigoLoja> lojas, Set<String> chavesItad) {
        if (chavesItad.isEmpty()) {
            throw new IllegalArgumentException("censo de produtos exige ao menos uma chave ITAD");
        }
        return new Cobertura(lojas, chavesItad);
    }

    public boolean ehCenso() {
        return !lojasEmCenso.isEmpty();
    }

    public boolean cobre(CodigoLoja loja, String chaveItadDoProduto) {
        if (!lojasEmCenso.contains(loja)) {
            return false;
        }
        if (chavesItadEmCenso.isEmpty()) {
            return true;
        }
        return chaveItadDoProduto != null && chavesItadEmCenso.contains(chaveItadDoProduto);
    }
}
