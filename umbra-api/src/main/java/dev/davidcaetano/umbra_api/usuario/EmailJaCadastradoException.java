package dev.davidcaetano.umbra_api.usuario;

import dev.davidcaetano.umbra_api.comum.excecao.RecursoEmConflitoException;

public class EmailJaCadastradoException extends RecursoEmConflitoException {

    public EmailJaCadastradoException() {
        super("Já existe uma conta com este email.");
    }
}
