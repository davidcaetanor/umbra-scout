package dev.davidcaetano.umbra_api.catalogo.service;

import java.util.Optional;

public interface FonteDeQualidade {

    Optional<QualidadeDoJogo> consultar(String chaveItad);
}
