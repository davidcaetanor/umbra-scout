package dev.davidcaetano.umbra_api.comum.excecao;

public abstract class NaoAutenticadoException extends RuntimeException {

    protected NaoAutenticadoException(String detalhe) {
        super(detalhe);
    }
}
