package dev.davidcaetano.umbra_api.catalogo.service;

import java.util.Objects;

record CandidatoADestaque(long produtoId, String chaveDoJogo, String publicador, boolean qualificado) {

    CandidatoADestaque {
        Objects.requireNonNull(chaveDoJogo, "chaveDoJogo é obrigatória");
    }
}
