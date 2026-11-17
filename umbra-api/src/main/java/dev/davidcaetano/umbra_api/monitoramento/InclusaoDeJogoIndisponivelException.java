package dev.davidcaetano.umbra_api.monitoramento;

import dev.davidcaetano.umbra_api.comum.excecao.ServicoIndisponivelException;

import java.time.Duration;

public class InclusaoDeJogoIndisponivelException extends ServicoIndisponivelException {

    public InclusaoDeJogoIndisponivelException(Duration tentarDeNovoEm, Throwable causa) {
        super("Não foi possível incluir o jogo agora. Tente de novo em instantes.", tentarDeNovoEm, causa);
    }
}
