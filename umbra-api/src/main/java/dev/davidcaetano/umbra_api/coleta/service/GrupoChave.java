package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;

record GrupoChave(String chaveItad, CodigoLoja loja, String identificadorLoja) {

    static GrupoChave de(OfertaColetada oferta) {
        return oferta.chaveItad() != null
                ? new GrupoChave(oferta.chaveItad(), null, null)
                : new GrupoChave(null, oferta.loja(), oferta.identificadorLoja());
    }
}
