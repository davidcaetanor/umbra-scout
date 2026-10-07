package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum LojaItad {

    STEAM(61, CodigoLoja.STEAM, "app/"),
    NUUVEM(50, CodigoLoja.NUUVEM, null),
    GOG(35, CodigoLoja.GOG, null),
    EPIC(16, CodigoLoja.EPIC, null),
    GREEN_MAN_GAMING(36, CodigoLoja.GREEN_MAN_GAMING, null);

    private final int shopId;
    private final CodigoLoja codigoLoja;
    private final String prefixoIdentificador;

    LojaItad(int shopId, CodigoLoja codigoLoja, String prefixoIdentificador) {
        this.shopId = shopId;
        this.codigoLoja = codigoLoja;
        this.prefixoIdentificador = prefixoIdentificador;
    }

    public static List<Integer> shopIds() {
        return Arrays.stream(values())
                .map(loja -> loja.shopId)
                .toList();
    }

    public static Set<CodigoLoja> codigosLoja() {
        return Arrays.stream(values())
                .map(loja -> loja.codigoLoja)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Optional<CodigoLoja> codigoLojaPorShopId(int shopId) {
        return porShopId(shopId).map(loja -> loja.codigoLoja);
    }

    public static String identificadorNativo(int shopId, String identificadorItad) {
        return porShopId(shopId)
                .map(loja -> loja.prefixoIdentificador)
                .filter(identificadorItad::startsWith)
                .map(prefixo -> identificadorItad.substring(prefixo.length()))
                .orElse(identificadorItad);
    }

    private static Optional<LojaItad> porShopId(int shopId) {
        return Arrays.stream(values())
                .filter(loja -> loja.shopId == shopId)
                .findFirst();
    }
}
