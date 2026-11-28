package dev.davidcaetano.umbra_api.notificacao;

import java.util.List;
import java.util.Objects;

public record DestinatarioEmEnvio(Long usuarioId, String email, List<AlvoEmEnvio> alvos) {

    public DestinatarioEmEnvio {
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        Objects.requireNonNull(email, "email é obrigatório");
        alvos = List.copyOf(alvos);
    }
}
