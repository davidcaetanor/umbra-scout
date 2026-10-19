package dev.davidcaetano.umbra_api.coleta;

import java.util.function.Predicate;

public class ColetaRetryPredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable falha) {
        return falha instanceof ColetaApiException coleta && coleta.transitoria();
    }
}
