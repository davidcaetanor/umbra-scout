package dev.davidcaetano.umbra_api.usuario.service;

public class ConsultaDeCepIndisponivelException extends RuntimeException {

    public ConsultaDeCepIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public ConsultaDeCepIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
