package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum LojaItad {

    STEAM(61, CodigoLoja.STEAM),
    NUUVEM(50, CodigoLoja.NUUVEM);

    private final int shopId;
    private final CodigoLoja codigoLoja;

    LojaItad(int shopId, CodigoLoja codigoLoja) {
        this.shopId = shopId;
        this.codigoLoja = codigoLoja;
    }

    public static List<Integer> shopIds() {
        return Arrays.stream(values())
                .map(loja -> loja.shopId)
                .toList();
    }

    public static Optional<CodigoLoja> codigoLojaPorShopId(int shopId) {
        return Arrays.stream(values())
                .filter(loja -> loja.shopId == shopId)
                .map(loja -> loja.codigoLoja)
                .findFirst();
    }
}
