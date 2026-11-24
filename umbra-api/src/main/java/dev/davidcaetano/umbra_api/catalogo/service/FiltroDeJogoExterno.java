package dev.davidcaetano.umbra_api.catalogo.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class FiltroDeJogoExterno {

    private FiltroDeJogoExterno() {
    }

    static Optional<JogoExterno> aplicar(JogoExterno jogo, FiltroDoCatalogo filtro) {
        List<OfertaExterna> consideradas = filtro.loja() == null
                ? jogo.ofertas()
                : jogo.ofertas().stream().filter(oferta -> oferta.codigoLoja() == filtro.loja()).toList();

        return consideradas.stream()
                .min(Comparator.comparingLong(OfertaExterna::valorCentavos))
                .filter(melhor -> dentroDaFaixa(melhor, filtro) && comDescontoMinimo(melhor, filtro))
                .map(melhor -> new JogoExterno(jogo.gid(), jogo.nome(), jogo.imagemUrl(), consideradas));
    }

    private static boolean dentroDaFaixa(OfertaExterna melhor, FiltroDoCatalogo filtro) {
        return (filtro.precoMinCentavos() == null || melhor.valorCentavos() >= filtro.precoMinCentavos())
                && (filtro.precoMaxCentavos() == null || melhor.valorCentavos() <= filtro.precoMaxCentavos());
    }

    private static boolean comDescontoMinimo(OfertaExterna melhor, FiltroDoCatalogo filtro) {
        return filtro.descontoMin() == null
                || melhor.descontoPct() != null && melhor.descontoPct() >= filtro.descontoMin();
    }
}
