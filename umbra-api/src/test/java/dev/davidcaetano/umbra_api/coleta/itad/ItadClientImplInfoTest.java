package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadAvaliacaoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoInfoResponse;
import dev.davidcaetano.umbra_api.comum.WireMockIntegrationTestBase;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItadClientImplInfoTest extends WireMockIntegrationTestBase {

    private static final UUID GID = UUID.fromString("018d937f-11e3-72e7-9b44-7a2a5c3b0c9b");

    // Formato de GET /games/info/v2 sondado em 27/11/2026 (Hollow Knight), reduzido aos campos lidos
    private static final String FIXTURE_INFO = """
            {
              "id": "018d937f-11e3-72e7-9b44-7a2a5c3b0c9b",
              "slug": "hollow-knight",
              "title": "Hollow Knight",
              "type": "game",
              "mature": false,
              "assets": { "boxart": "https://assets.isthereanydeal.com/018d937f-11e3-72e7-9b44-7a2a5c3b0c9b/boxart.jpg" },
              "tags": ["Metroidvania", "Platformer"],
              "releaseDate": "2017-02-24",
              "developers": [{ "id": 1, "name": "Team Cherry" }],
              "publishers": [{ "id": 1, "name": "Team Cherry" }],
              "reviews": [
                { "score": 96, "source": "Steam", "count": 489363, "url": "https://store.steampowered.com/app/367520/" },
                { "score": 87, "source": "Metascore", "count": 27, "url": "https://metacritic.com/game/hollow-knight/" },
                { "score": 90, "source": "Metacritic User Score", "count": 6490, "url": "https://metacritic.com/game/hollow-knight/" },
                { "score": 89, "source": "OpenCritic", "count": 76, "url": "https://opencritic.com/game/4002/hollow-knight" }
              ],
              "stats": { "rank": 15, "waitlisted": 19727, "collected": 53017 }
            }
            """;

    @Autowired
    private ItadClient itadClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void buscarInfo_deveLerAvaliacoesEstatisticasEPublicador() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/info/v2"))
                .withQueryParam("id", equalTo(GID.toString()))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_INFO)));

        ItadJogoInfoResponse info = itadClient.buscarInfo(GID).orElseThrow();

        assertThat(info.reviews()).extracting(ItadAvaliacaoResponse::source)
                .containsExactly("Steam", "Metascore", "Metacritic User Score", "OpenCritic");
        assertThat(info.reviews().getFirst()).isEqualTo(new ItadAvaliacaoResponse(96, "Steam", 489363));
        assertThat(info.stats().waitlisted()).isEqualTo(19727);
        assertThat(info.publishers()).singleElement().satisfies(empresa -> assertThat(empresa.name()).isEqualTo("Team Cherry"));
    }

    @Test
    void buscarInfo_jogoInexistente_deveVirVazioSemContarComoFalha() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/info/v2"))
                .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json")
                        .withBody("{\"status_code\":404,\"reason_phrase\":\"Not Found\"}")));

        assertThat(itadClient.buscarInfo(GID)).isEmpty();
        assertThat(circuitBreakerRegistry.circuitBreaker("itad-qualidade").getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void buscarInfo_falhaSeguida_abreSoOCircuitoDaQualidadeSemRetentar() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/info/v2")).willReturn(aResponse().withStatus(500)));

        for (int tentativa = 0; tentativa < 3; tentativa++) {
            assertThatThrownBy(() -> itadClient.buscarInfo(GID)).isInstanceOf(ItadApiException.class);
        }

        WIREMOCK.verify(3, getRequestedFor(urlPathEqualTo("/games/info/v2")));
        assertThat(circuitBreakerRegistry.circuitBreaker("itad-qualidade").getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreakerRegistry.circuitBreaker("itad").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
