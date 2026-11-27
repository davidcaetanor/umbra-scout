package dev.davidcaetano.umbra_api.monitoramento;

import dev.davidcaetano.umbra_api.comum.excecao.RegraDeNegocioVioladaException;

public class TetoDeMonitoramentosAtingidoException extends RegraDeNegocioVioladaException {

    public TetoDeMonitoramentosAtingidoException(int teto) {
        super("Você chegou ao limite de %d monitoramentos. Remova um para incluir outro.".formatted(teto));
    }
}
