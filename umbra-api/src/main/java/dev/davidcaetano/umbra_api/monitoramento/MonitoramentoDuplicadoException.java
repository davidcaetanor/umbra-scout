package dev.davidcaetano.umbra_api.monitoramento;

import dev.davidcaetano.umbra_api.comum.excecao.RecursoEmConflitoException;

public class MonitoramentoDuplicadoException extends RecursoEmConflitoException {

    public MonitoramentoDuplicadoException() {
        super("Este produto já está nos seus monitoramentos.");
    }
}
