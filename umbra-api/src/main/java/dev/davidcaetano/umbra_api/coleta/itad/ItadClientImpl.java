package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadDescobertaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadPrecoJogoResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.davidcaetano.umbra_api.coleta.itad.TraducaoDeErroItad.executar;

@Service
@RequiredArgsConstructor
public class ItadClientImpl implements ItadClient {

    private static final String COUNTRY = "BR";
    private static final int LIMITE_DESCOBERTA = 200;
    private static final String ORDENACAO_DESCOBERTA = "-trending";

    private final RestClient itadRestClient;

    @Override
    @Retry(name = "itad")
    @CircuitBreaker(name = "itad")
    @RateLimiter(name = "itad-coleta")
    public ItadDescobertaResponse buscarDescoberta(int offset) {
        return executar(() -> itadRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/deals/v2")
                        .queryParam("country", COUNTRY)
                        .queryParam("shops", juntar(LojaItad.shopIds()))
                        .queryParam("nondeals", false)
                        .queryParam("offset", offset)
                        .queryParam("limit", LIMITE_DESCOBERTA)
                        .queryParam("sort", ORDENACAO_DESCOBERTA)
                        .build())
                .retrieve()
                .body(ItadDescobertaResponse.class));
    }

    @Override
    @Retry(name = "itad")
    @CircuitBreaker(name = "itad")
    @RateLimiter(name = "itad-coleta")
    public Map<UUID, List<String>> resolverIdentificadorNativo(int shopId, List<UUID> gids) {
        Map<UUID, List<String>> resposta = executar(() -> itadRestClient.post()
                .uri("/lookup/shop/{shopId}/id/v1", shopId)
                .body(gids)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<UUID, List<String>>>() {
                }));

        return resposta == null ? Map.of() : resposta;
    }

    @Override
    @Retry(name = "itad")
    @CircuitBreaker(name = "itad")
    @RateLimiter(name = "itad-coleta")
    public Map<String, UUID> resolverGidPorIdentificador(int shopId, List<String> identificadoresItad) {
        Map<String, UUID> resposta = executar(() -> itadRestClient.post()
                .uri("/lookup/id/shop/{shopId}/v1", shopId)
                .body(identificadoresItad)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, UUID>>() {
                }));

        return resposta == null ? Map.of() : resposta;
    }

    @Override
    @Retry(name = "itad")
    @CircuitBreaker(name = "itad")
    @RateLimiter(name = "itad-coleta")
    public List<ItadPrecoJogoResponse> buscarPrecos(List<UUID> gids) {
        List<ItadPrecoJogoResponse> resposta = executar(() -> itadRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/prices/v3")
                        .queryParam("country", COUNTRY)
                        .queryParam("shops", juntar(LojaItad.shopIds()))
                        .build())
                .body(gids)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ItadPrecoJogoResponse>>() {
                }));

        return resposta == null ? List.of() : resposta;
    }

    private static String juntar(List<Integer> shopIds) {
        return shopIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
