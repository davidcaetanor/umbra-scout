package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoBuscaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoInfoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadPrecoJogoResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.davidcaetano.umbra_api.coleta.itad.TraducaoDeErroItad.executar;

@Component
@RequiredArgsConstructor
public class ItadBuscaClient {

    private static final String COUNTRY = "BR";
    private static final int RESULTADOS_DA_BUSCA = 20;

    private final RestClient itadBuscaRestClient;

    @CircuitBreaker(name = "itad-busca")
    @RateLimiter(name = "itad-busca")
    public List<ItadJogoBuscaResponse> buscarJogos(String termo) {
        List<ItadJogoBuscaResponse> resposta = executar(() -> itadBuscaRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/search/v1")
                        .queryParam("title", termo)
                        .queryParam("results", RESULTADOS_DA_BUSCA)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<ItadJogoBuscaResponse>>() {
                }));

        return resposta == null ? List.of() : resposta;
    }

    @CircuitBreaker(name = "itad-busca")
    @RateLimiter(name = "itad-busca")
    public List<ItadPrecoJogoResponse> buscarPrecos(List<UUID> gids) {
        List<ItadPrecoJogoResponse> resposta = executar(() -> itadBuscaRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/prices/v3")
                        .queryParam("country", COUNTRY)
                        .queryParam("shops", LojaItad.shopIds().stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(",")))
                        .build())
                .body(gids)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ItadPrecoJogoResponse>>() {
                }));

        return resposta == null ? List.of() : resposta;
    }

    @CircuitBreaker(name = "itad-busca")
    @RateLimiter(name = "itad-busca")
    public Optional<ItadJogoInfoResponse> buscarInfo(UUID gid) {
        try {
            return Optional.ofNullable(executar(() -> itadBuscaRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/games/info/v2").queryParam("id", gid).build())
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (pedido, resposta) -> {
                        throw new JogoAusenteNoItad();
                    })
                    .body(ItadJogoInfoResponse.class)));
        } catch (JogoAusenteNoItad ausente) {
            return Optional.empty();
        }
    }

    @CircuitBreaker(name = "itad-busca")
    @RateLimiter(name = "itad-busca")
    public Map<UUID, List<String>> resolverIdentificadorNativo(int shopId, List<UUID> gids) {
        Map<UUID, List<String>> resposta = executar(() -> itadBuscaRestClient.post()
                .uri("/lookup/shop/{shopId}/id/v1", shopId)
                .body(gids)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<UUID, List<String>>>() {
                }));

        return resposta == null ? Map.of() : resposta;
    }

    private static final class JogoAusenteNoItad extends RuntimeException {

        private JogoAusenteNoItad() {
            super(null, null, false, false);
        }
    }
}
