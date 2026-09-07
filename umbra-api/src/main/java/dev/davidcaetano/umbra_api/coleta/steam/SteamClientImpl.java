package dev.davidcaetano.umbra_api.coleta.steam;

import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamAppDetalhesResponse;
import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamDescobertaResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SteamClientImpl implements SteamClient {

    private static final String COUNTRY = "br";
    private static final String LANGUAGE = "portuguese";

    private final RestClient steamRestClient;

    @Override
    @RateLimiter(name = "steam")
    public SteamDescobertaResponse buscarDescoberta() {
        try {
            return steamRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/featuredcategories")
                            .queryParam("cc", COUNTRY)
                            .queryParam("l", LANGUAGE)
                            .build())
                    .retrieve()
                    .body(SteamDescobertaResponse.class);
        } catch (RestClientException ex) {
            throw traduzirErro(ex);
        }
    }

    @Override
    @RateLimiter(name = "steam")
    public SteamAppDetalhesResponse buscarDetalhes(int appid) {
        try {
            Map<String, SteamAppDetalhesResponse> mapa = steamRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/appdetails")
                            .queryParam("appids", appid)
                            .queryParam("cc", COUNTRY)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, SteamAppDetalhesResponse>>() {
                    });

            return mapa == null ? null : mapa.get(String.valueOf(appid));
        } catch (RestClientException ex) {
            throw traduzirErro(ex);
        }
    }

    private SteamApiException traduzirErro(RestClientException ex) {
        if (ex instanceof RestClientResponseException responseEx) {
            String corpo = responseEx.getResponseBodyAsString();
            String mensagem = corpo.isBlank() ? responseEx.getMessage() : corpo;

            log.warn("Steam Storefront API respondeu erro: {} - {}", responseEx.getStatusCode(), mensagem);
            return new SteamApiException(responseEx.getStatusCode().value(), mensagem, ex);
        }

        log.warn("Steam Storefront API devolveu corpo que nao pode ser convertido: {}", ex.getMessage());
        return new SteamApiException(0, ex.getMessage(), ex);
    }
}
