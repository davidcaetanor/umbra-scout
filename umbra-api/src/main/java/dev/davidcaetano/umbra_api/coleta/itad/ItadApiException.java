package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.coleta.ColetaApiException;
import org.springframework.web.client.RestClientException;

public class ItadApiException extends ColetaApiException {

    public ItadApiException(String detalhe, RestClientException causa) {
        super("ITAD API", detalhe, causa);
    }
}
