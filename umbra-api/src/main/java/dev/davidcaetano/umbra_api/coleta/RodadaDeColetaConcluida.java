package dev.davidcaetano.umbra_api.coleta;

import java.util.Objects;

public record RodadaDeColetaConcluida(String tarefa) {

    public RodadaDeColetaConcluida {
        Objects.requireNonNull(tarefa, "tarefa é obrigatória");
    }
}
