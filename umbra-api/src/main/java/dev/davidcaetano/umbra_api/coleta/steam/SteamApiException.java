package dev.davidcaetano.umbra_api.coleta.steam;

import dev.davidcaetano.umbra_api.coleta.ColetaApiException;
import org.springframework.web.client.RestClientException;

public class SteamApiException extends ColetaApiException {

    public SteamApiException(String detalhe, RestClientException causa) {
        super("Steam Storefront API", detalhe, causa);
    }
}
