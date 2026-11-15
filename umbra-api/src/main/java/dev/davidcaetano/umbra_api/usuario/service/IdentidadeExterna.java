package dev.davidcaetano.umbra_api.usuario.service;

import java.util.Objects;

public record IdentidadeExterna(ProvedorDeIdentidade provedor, String idExterno, String email,
                                boolean emailVerificado, String nome) {

    public IdentidadeExterna {
        Objects.requireNonNull(provedor, "provedor é obrigatório");
        Objects.requireNonNull(idExterno, "idExterno é obrigatório");
    }

    @Override
    public String toString() {
        return "IdentidadeExterna[provedor=" + provedor + ", idExterno=" + idExterno + "]";
    }
}
