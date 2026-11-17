package dev.davidcaetano.umbra_api.comum.excecao;

import java.time.Duration;
import java.util.Objects;

public abstract class ServicoIndisponivelException extends RuntimeException {

    private final Duration tentarDeNovoEm;

    protected ServicoIndisponivelException(String detalhe, Duration tentarDeNovoEm, Throwable causa) {
        super(detalhe, causa);
        this.tentarDeNovoEm = Objects.requireNonNull(tentarDeNovoEm, "tentarDeNovoEm é obrigatório");
    }

    public Duration tentarDeNovoEm() {
        return tentarDeNovoEm;
    }
}
