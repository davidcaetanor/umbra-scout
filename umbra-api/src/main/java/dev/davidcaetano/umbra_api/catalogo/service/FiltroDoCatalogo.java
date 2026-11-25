package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.enums.CategoriaHardware;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;

public record FiltroDoCatalogo(String termo,
                               TipoProduto tipo,
                               CategoriaHardware categoria,
                               Long precoMinCentavos,
                               Long precoMaxCentavos,
                               Integer descontoMin,
                               CodigoLoja loja) {

    public static FiltroDoCatalogo dosDestaques() {
        return new FiltroDoCatalogo(null, TipoProduto.JOGO, null, null, null, null, null);
    }

    public boolean admiteJogo() {
        return tipo != TipoProduto.HARDWARE && categoria == null;
    }
}
