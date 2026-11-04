package dev.davidcaetano.umbra_api.comum.excecao;

public abstract class RecursoNaoEncontradoException extends RuntimeException {

    protected RecursoNaoEncontradoException(String detalhe) {
        super(detalhe);
    }
}
