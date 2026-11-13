package dev.davidcaetano.umbra_api.usuario;

import dev.davidcaetano.umbra_api.comum.excecao.RecursoNaoEncontradoException;

public class UsuarioNaoEncontradoException extends RecursoNaoEncontradoException {

    public UsuarioNaoEncontradoException() {
        super("Usuário não encontrado.");
    }
}
