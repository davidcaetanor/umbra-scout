package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.service.BuscaExterna;
import dev.davidcaetano.umbra_api.catalogo.service.BuscaExternaIndisponivelException;
import dev.davidcaetano.umbra_api.catalogo.service.JogoExterno;
import dev.davidcaetano.umbra_api.catalogo.service.OfertaExterna;
import dev.davidcaetano.umbra_api.comum.WireMockIntegrationTestBase;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItadBuscaExternaTest extends WireMockIntegrationTestBase {

    private static final String FIXTURE_BUSCA = fixture("wiremock/itad/games-search-v1-elden-ring.json");
    private static final String FIXTURE_PRECOS = fixture("wiremock/itad/games-prices-v3-elden-ring.json");

    private static final String GID_ELDEN_RING = "018d937f-590c-728b-ac35-38bcff85f086";
    private static final String GID_NIGHTREIGN = "0193bdcf-1842-73b6-aa11-bf0ebd540557";
    private static final String GID_NIGHTREIGN_DELUXE = "0194fa78-67be-7370-81b0-53debfa591b9";
    private static final String URL_ELDEN_RING_STEAM = "https://itad.link/018d9386-5633-7086-a2fa-f903c6afc45b/?app=tqtwff";
    private static final String URL_ELDEN_RING_NUUVEM = "https://itad.link/018d9386-5633-7086-a2fa-f903b523c3e1/?app=tqtwff";

    @Autowired
    private BuscaExterna buscaExterna;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void buscar_deveDevolverSoOsJogosDoTipoGameComAsOfertasEmBrl() {
        stubBusca("t212c-tipo", FIXTURE_BUSCA);
        stubPrecos(FIXTURE_PRECOS);

        List<JogoExterno> jogos = buscaExterna.buscar("t212c-tipo");

        assertThat(jogos).extracting(JogoExterno::gid)
                .containsExactly(GID_ELDEN_RING, GID_NIGHTREIGN, GID_NIGHTREIGN_DELUXE);
        assertThat(jogos.getFirst().nome()).isEqualTo("Elden Ring");
        assertThat(jogos.getFirst().imagemUrl()).contains(GID_ELDEN_RING);
        assertThat(jogos.getFirst().ofertas()).containsExactly(
                new OfertaExterna(CodigoLoja.STEAM, URL_ELDEN_RING_STEAM, 27450, 27450L, (short) 0, null),
                new OfertaExterna(CodigoLoja.NUUVEM, URL_ELDEN_RING_NUUVEM, 24705, 27450L, (short) 10, null));
        WIREMOCK.verify(getRequestedFor(urlPathEqualTo("/games/search/v1"))
                .withQueryParam("title", equalTo("t212c-tipo"))
                .withQueryParam("results", equalTo("20")));
        WIREMOCK.verify(postRequestedFor(urlPathEqualTo("/games/prices/v3"))
                .withQueryParam("country", equalTo("BR"))
                .withQueryParam("shops", equalTo("61,50,35,16,36")));
    }

    @Test
    void buscar_deveDeixarDeForaDlcPacoteETipoNulo() {
        stubBusca("t212c-fora", FIXTURE_BUSCA);
        stubPrecos(FIXTURE_PRECOS);

        assertThat(buscaExterna.buscar("t212c-fora")).extracting(JogoExterno::gid).doesNotContain(
                "018dcc3c-5be6-7113-97c8-380547ec6cc3",
                "018dcc3c-39cd-73b7-a1f9-592cc915d76c",
                "01a03461-5224-716e-8efc-9b2ac2ed8de4",
                "01972022-ad94-72d0-af45-f6985b8fc8ac");
    }

    @Test
    void buscar_lojaRepetida_deveFicarComAOfertaMaisBarata() {
        stubBusca("t212c-repetida", FIXTURE_BUSCA);
        stubPrecos("""
                [{"id": "%s", "deals": [
                  {"shop": {"id": 50, "name": "Nuuvem"}, "price": {"amountInt": 30000, "currency": "BRL"},
                   "regular": {"amountInt": 30000, "currency": "BRL"}, "cut": 0, "url": "https://itad.link/cara"},
                  {"shop": {"id": 50, "name": "Nuuvem"}, "price": {"amountInt": 24705, "currency": "BRL"},
                   "regular": {"amountInt": 27450, "currency": "BRL"}, "cut": 10, "url": "https://itad.link/barata"}
                ]}]
                """.formatted(GID_ELDEN_RING));

        List<JogoExterno> jogos = buscaExterna.buscar("t212c-repetida");

        assertThat(jogos).singleElement().satisfies(jogo -> assertThat(jogo.ofertas()).containsExactly(
                new OfertaExterna(CodigoLoja.NUUVEM, "https://itad.link/barata", 24705, 27450L, (short) 10, null)));
    }

    @Test
    void buscar_jogoSemOfertaEmBrl_deveFicarDeFora() {
        stubBusca("t212c-moeda", FIXTURE_BUSCA);
        stubPrecos("""
                [{"id": "%s", "deals": [
                  {"shop": {"id": 61, "name": "Steam"}, "price": {"amountInt": 5999, "currency": "USD"},
                   "regular": {"amountInt": 5999, "currency": "USD"}, "cut": 0, "url": "https://itad.link/usd"}
                ]}]
                """.formatted(GID_ELDEN_RING));

        assertThat(buscaExterna.buscar("t212c-moeda")).isEmpty();
    }

    @Test
    void buscar_semNenhumJogoDoTipoGame_naoDeveConsultarPrecos() {
        stubBusca("t212c-sem-jogo", """
                [{"id": "018dcc3c-5be6-7113-97c8-380547ec6cc3", "title": "DLC", "type": "dlc", "assets": {}}]
                """);

        assertThat(buscaExterna.buscar("t212c-sem-jogo")).isEmpty();
        WIREMOCK.verify(0, postRequestedFor(urlPathEqualTo("/games/prices/v3")));
    }

    @Test
    void erro500DoItad_deveVirarBuscaExternaIndisponivelComUmaRequisicaoSo() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/search/v1")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> buscaExterna.buscar("t212c-500"))
                .isInstanceOf(BuscaExternaIndisponivelException.class)
                .hasCauseInstanceOf(ItadApiException.class);
        WIREMOCK.verify(1, getRequestedFor(urlPathEqualTo("/games/search/v1")));
    }

    @Test
    void respostaAlemDoTimeoutDeLeitura_deveVirarBuscaExternaIndisponivel() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/search/v1"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_BUSCA).withFixedDelay(1_500)));

        assertThatThrownBy(() -> buscaExterna.buscar("t212c-lento"))
                .isInstanceOf(BuscaExternaIndisponivelException.class)
                .hasCauseInstanceOf(ItadApiException.class);
    }

    @Test
    void tresFalhas_devemAbrirOCircuitoDaBusca_eManterOCircuitoDaColetaFechado() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/search/v1")).willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> buscaExterna.buscar("t212c-circuito"))
                    .isInstanceOf(BuscaExternaIndisponivelException.class);
        }

        assertThat(circuitBreakerRegistry.circuitBreaker("itad-busca").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreakerRegistry.circuitBreaker("itad").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        assertThatThrownBy(() -> buscaExterna.buscar("t212c-circuito"))
                .isInstanceOf(BuscaExternaIndisponivelException.class);
        WIREMOCK.verify(3, getRequestedFor(urlPathEqualTo("/games/search/v1")));
    }

    private static void stubBusca(String termo, String corpo) {
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/search/v1"))
                .withQueryParam("title", equalTo(termo))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(corpo)));
    }

    private static void stubPrecos(String corpo) {
        WIREMOCK.stubFor(post(urlPathEqualTo("/games/prices/v3"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(corpo)));
    }
}
