package dev.davidcaetano.umbra_api.comum.excecao;

public abstract class RecursoEmConflitoException extends RuntimeException {

    protected RecursoEmConflitoException(String detalhe) {
        super(detalhe);
    }
}
