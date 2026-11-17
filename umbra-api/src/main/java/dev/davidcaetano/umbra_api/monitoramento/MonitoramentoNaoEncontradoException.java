package dev.davidcaetano.umbra_api.monitoramento;

import dev.davidcaetano.umbra_api.comum.excecao.RecursoNaoEncontradoException;

public class MonitoramentoNaoEncontradoException extends RecursoNaoEncontradoException {

    public MonitoramentoNaoEncontradoException(Long id) {
        super("Monitoramento %d não encontrado.".formatted(id));
    }
}
