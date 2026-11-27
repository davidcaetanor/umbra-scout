package dev.davidcaetano.umbra_api.catalogo;

import dev.davidcaetano.umbra_api.comum.excecao.ServicoIndisponivelException;

import java.time.Duration;

public class CatalogoSobrecarregadoException extends ServicoIndisponivelException {

    public CatalogoSobrecarregadoException(Duration tentarDeNovoEm, Throwable causa) {
        super("O catálogo está demorando para responder. Tente de novo em instantes.", tentarDeNovoEm, causa);
    }
}
