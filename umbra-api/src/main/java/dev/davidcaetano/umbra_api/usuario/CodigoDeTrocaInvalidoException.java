package dev.davidcaetano.umbra_api.usuario;

import dev.davidcaetano.umbra_api.comum.excecao.NaoAutenticadoException;

public class CodigoDeTrocaInvalidoException extends NaoAutenticadoException {

    public CodigoDeTrocaInvalidoException() {
        super("Código de login inválido ou expirado.");
    }
}
