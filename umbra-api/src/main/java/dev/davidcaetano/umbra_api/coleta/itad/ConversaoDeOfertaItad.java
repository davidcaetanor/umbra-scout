package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadOfertaPrecoResponse;

import java.util.UUID;

final class ConversaoDeOfertaItad {

    private ConversaoDeOfertaItad() {
    }

    static OfertaColetada paraOfertaColetada(CodigoLoja loja, String identificadorLoja, String titulo,
                                             String imagemUrl, UUID gid, ItadOfertaPrecoResponse deal) {
        return new OfertaColetada(
                loja,
                identificadorLoja,
                TipoProduto.JOGO,
                titulo,
                null,
                deal.url(),
                imagemUrl,
                gid.toString(),
                deal.price().amountInt(),
                (long) deal.regular().amountInt(),
                (short) deal.cut(),
                true,
                OrigemColeta.ITAD_API,
                deal.expiry()
        );
    }
}
