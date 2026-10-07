package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadOfertaPrecoResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


final class MenorPrecoPorLoja {

    private MenorPrecoPorLoja() {
    }

    static List<ItadOfertaPrecoResponse> escolher(List<ItadOfertaPrecoResponse> ofertas) {
        List<ItadOfertaPrecoResponse> escolhidas = new ArrayList<>();
        Map<Integer, Integer> posicaoPorShop = new HashMap<>();

        for (ItadOfertaPrecoResponse oferta : ofertas) {
            if (oferta.shop() == null || oferta.price() == null) {
                escolhidas.add(oferta);
                continue;
            }

            Integer posicao = posicaoPorShop.get(oferta.shop().id());
            if (posicao == null) {
                posicaoPorShop.put(oferta.shop().id(), escolhidas.size());
                escolhidas.add(oferta);
            } else if (oferta.price().amountInt() < escolhidas.get(posicao).price().amountInt()) {
                escolhidas.set(posicao, oferta);
            }
        }

        return escolhidas;
    }
}
