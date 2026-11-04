package dev.davidcaetano.umbra_api.catalogo;

import dev.davidcaetano.umbra_api.comum.excecao.RecursoNaoEncontradoException;

public class ProdutoNaoEncontradoException extends RecursoNaoEncontradoException {

    public ProdutoNaoEncontradoException(Long id) {
        super("Produto %d não encontrado.".formatted(id));
    }
}
