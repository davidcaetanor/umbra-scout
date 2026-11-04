package dev.davidcaetano.umbra_api.catalogo.service;

import java.util.Arrays;

public enum OrdenacaoCatalogo {

    NOME("nome"),
    MENOR_PRECO("menorPreco");

    private final String codigo;

    OrdenacaoCatalogo(String codigo) {
        this.codigo = codigo;
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
