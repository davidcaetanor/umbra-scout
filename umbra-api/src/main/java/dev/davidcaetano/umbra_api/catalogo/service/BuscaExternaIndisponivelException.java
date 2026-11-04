package dev.davidcaetano.umbra_api.catalogo.service;

public class BuscaExternaIndisponivelException extends RuntimeException {

    public BuscaExternaIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public BuscaExternaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
