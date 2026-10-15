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
import java.util.function.Supplier;

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
        return executar(() -> steamRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/featuredcategories")
                        .queryParam("cc", COUNTRY)
                        .queryParam("l", LANGUAGE)
                        .build())
                .retrieve()
                .body(SteamDescobertaResponse.class));
    }

    @Override
    @RateLimiter(name = "steam")
    public SteamAppDetalhesResponse buscarDetalhes(int appid) {
        Map<String, SteamAppDetalhesResponse> mapa = executar(() -> steamRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/appdetails")
                        .queryParam("appids", appid)
                        .queryParam("cc", COUNTRY)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, SteamAppDetalhesResponse>>() {
                }));

        return mapa == null ? null : mapa.get(String.valueOf(appid));
    }

    private <T> T executar(Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (RestClientException ex) {
            throw traduzirErro(ex);
        }
    }

    private SteamApiException traduzirErro(RestClientException ex) {
        SteamApiException erro = new SteamApiException(detalhe(ex), ex);
        log.warn("{}", erro.getMessage());
        return erro;
    }

    private static String detalhe(RestClientException ex) {
        if (ex instanceof RestClientResponseException resposta && !resposta.getResponseBodyAsString().isBlank()) {
            return resposta.getResponseBodyAsString();
        }
        return ex.getMessage();
    }
}
