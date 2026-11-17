package dev.davidcaetano.umbra_api.monitoramento;

import dev.davidcaetano.umbra_api.comum.excecao.RegraDeNegocioVioladaException;

public class AlvoEmProdutoGratuitoException extends RegraDeNegocioVioladaException {

    public AlvoEmProdutoGratuitoException() {
        super("Produto gratuito não aceita preço alvo.");
    }
}
