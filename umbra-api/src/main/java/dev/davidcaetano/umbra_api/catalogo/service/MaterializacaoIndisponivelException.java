package dev.davidcaetano.umbra_api.catalogo.service;

public class MaterializacaoIndisponivelException extends RuntimeException {

    public MaterializacaoIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public MaterializacaoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
