package dev.davidcaetano.umbra_api.usuario.service;

import java.util.Objects;

public record UsuarioAutenticado(Long id) {

    public UsuarioAutenticado {
        Objects.requireNonNull(id, "id é obrigatório");
    }
}
