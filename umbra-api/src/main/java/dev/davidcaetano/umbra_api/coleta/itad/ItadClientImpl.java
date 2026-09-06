package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadDescobertaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadErroResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadPrecoJogoResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItadClientImpl implements ItadClient {

    private static final String COUNTRY = "BR";

    //Steam = 61, Nuuvem = 50

    private static final List<Integer> SHOPS = List.of(61, 50);

    private final RestClient itadRestClient;

    @Override
    @RateLimiter(name = "itad")
    public ItadDescobertaResponse buscarDescoberta(List<Integer> shopIds, int offset) {
        try {
            return itadRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/deals/v2")
                            .queryParam("country", COUNTRY)
                            .queryParam("shops", juntar(shopIds))
                            .queryParam("nondeals", false)
                            .queryParam("offset", offset)
                            .build())
                    .retrieve()
                    .body(ItadDescobertaResponse.class);
        } catch (RestClientResponseException ex) {
            throw traduzirErro(ex);
        }
    }

    @Override
    @RateLimiter(name = "itad")
    public Map<UUID, List<String>> resolverIdentificadorNativo(int shopId, List<UUID> gids) {
        try {
            Map<UUID, List<String>> resposta = itadRestClient.post()
                    .uri("/lookup/shop/{shopId}/id/v1", shopId)
                    .body(gids)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<UUID, List<String>>>() {
                    });

            return resposta == null ? Map.of() : resposta;
        } catch (RestClientResponseException ex) {
            throw traduzirErro(ex);
        }
    }

    @Override
    @RateLimiter(name = "itad")
    public List<ItadPrecoJogoResponse> buscarPrecos(List<UUID> gids) {
        try {
            List<ItadPrecoJogoResponse> resposta = itadRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/games/prices/v3")
                            .queryParam("country", COUNTRY)
                            .queryParam("shops", juntar(SHOPS))
                            .build())
                    .body(gids)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ItadPrecoJogoResponse>>() {
                    });

            return resposta == null ? List.of() : resposta;
        } catch (RestClientResponseException ex) {
            throw traduzirErro(ex);
        }
    }

    private ItadApiException traduzirErro(RestClientResponseException ex) {
        ItadErroResponse erro = null;
        try {
            erro = ex.getResponseBodyAs(ItadErroResponse.class);
        } catch (RuntimeException naoDecodificou) {
            log.warn("Corpo de erro do ITAD nao pode ser decodificado como ItadErroResponse", naoDecodificou);
        }

        if (erro != null) {
            log.warn("ITAD respondeu erro: {} - {}", erro.statusCode(), erro.reasonPhrase());
            return new ItadApiException(erro.statusCode(), erro.reasonPhrase(), ex);
        }

        log.warn("ITAD respondeu erro sem corpo decodificavel: {}", ex.getStatusCode());
        return new ItadApiException(ex.getStatusCode().value(), ex.getMessage(), ex);
    }

    private static String juntar(List<Integer> shopIds) {
        return shopIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
