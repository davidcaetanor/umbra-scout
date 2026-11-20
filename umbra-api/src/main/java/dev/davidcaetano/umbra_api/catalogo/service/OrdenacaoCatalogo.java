package dev.davidcaetano.umbra_api.catalogo.service;

import java.util.Arrays;

public enum OrdenacaoCatalogo {

    NOME("nome", false),
    MENOR_PRECO("menorPreco", false),
    MELHORES_OFERTAS("melhoresOfertas", true),
    MAIOR_DESCONTO("maiorDesconto", true);

    private final String codigo;
    private final boolean exclusivaDeJogo;

    OrdenacaoCatalogo(String codigo, boolean exclusivaDeJogo) {
        this.codigo = codigo;
        this.exclusivaDeJogo = exclusivaDeJogo;
    }

    public boolean exclusivaDeJogo() {
        return exclusivaDeJogo;
    }

    public static OrdenacaoCatalogo deCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(ordenacao -> ordenacao.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ordenação desconhecida: " + codigo));
    }

    @Override
    public String toString() {
        return codigo;
    }
}
