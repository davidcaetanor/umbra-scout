package dev.davidcaetano.umbra_api.catalogo.service;

public record QualidadeDoJogo(Integer steamNota,
                              Integer steamAvaliacoes,
                              Integer criticaNota,
                              Integer criticaAvaliacoes,
                              Integer listaDesejos,
                              String publicador) {

    public static QualidadeDoJogo semAvaliacao() {
        return new QualidadeDoJogo(null, null, null, null, null, null);
    }
}
