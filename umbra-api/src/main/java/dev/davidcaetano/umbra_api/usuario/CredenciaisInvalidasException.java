package dev.davidcaetano.umbra_api.usuario;

import dev.davidcaetano.umbra_api.comum.excecao.NaoAutenticadoException;

public class CredenciaisInvalidasException extends NaoAutenticadoException {

    public CredenciaisInvalidasException() {
        super("Email ou senha inválidos.");
    }
}
