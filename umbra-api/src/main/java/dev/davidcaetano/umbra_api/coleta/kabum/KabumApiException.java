package dev.davidcaetano.umbra_api.coleta.kabum;

import dev.davidcaetano.umbra_api.coleta.ColetaApiException;
import org.springframework.web.client.RestClientException;

public class KabumApiException extends ColetaApiException {

    public KabumApiException(String detalhe, RestClientException causa) {
        super("Kabum Catalog API", detalhe, causa);
    }
}
