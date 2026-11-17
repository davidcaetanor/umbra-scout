package dev.davidcaetano.umbra_api.catalogo;

import dev.davidcaetano.umbra_api.comum.excecao.RecursoNaoEncontradoException;

public class JogoExternoNaoEncontradoException extends RecursoNaoEncontradoException {

    public JogoExternoNaoEncontradoException(String gid) {
        super("Jogo %s não encontrado.".formatted(gid));
    }
}
