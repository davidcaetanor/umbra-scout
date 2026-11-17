package dev.davidcaetano.umbra_api.comum.excecao;

public abstract class RegraDeNegocioVioladaException extends RuntimeException {

    protected RegraDeNegocioVioladaException(String detalhe) {
        super(detalhe);
    }
}
