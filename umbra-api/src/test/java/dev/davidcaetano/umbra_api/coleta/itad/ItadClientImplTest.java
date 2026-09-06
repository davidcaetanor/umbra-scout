package dev.davidcaetano.umbra_api.coleta.itad;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadDescobertaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoDescobertoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadPrecoJogoResponse;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ItadClientImplTest extends IntegrationTestBase {

    @RegisterExtension
    static WireMockExtension WIREMOCK = WireMockExtension.newInstance()

            .options(WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true))
            .build();

    @DynamicPropertySource
    static void configurarBaseUrlItad(DynamicPropertyRegistry registry) {
        registry.add("itad.itad-api.base-url", WIREMOCK::baseUrl);
    }

    @Autowired
    private ItadClient itadClient;

    private static final UUID GID_JOGO = UUID.fromString("018d937e-f8d2-7123-9942-85ddaff9efee");

    // Fixture 1 — GET /deals/v2?shops=50 (real, Nuuvem, capturada em 05/09/2026)
    private static final String FIXTURE_DESCOBERTA = """
            {
              "nextOffset": 5,
              "hasMore": true,
              "list": [
                {
                  "id": "018d937e-f014-7064-9a7c-b86cdbd879d7",
                  "slug": "bighead-runner-soundtrack",
                  "title": "Bighead Runner - Soundtrack",
                  "type": "dlc",
                  "mature": false,
                  "assets": { "boxart": "https://assets.isthereanydeal.com/018d937e-f014-7064-9a7c-b86cdbd879d7/boxart.jpg?t=1783716626" },
                  "deal": {
                    "shop": { "id": 50, "name": "Nuuvem" },
                    "price": { "amount": 0.37, "amountInt": 37, "currency": "BRL" },
                    "regular": { "amount": 2.29, "amountInt": 229, "currency": "BRL" },
                    "cut": 84, "voucher": null,
                    "storeLow": { "amount": 0.37, "amountInt": 37, "currency": "BRL" },
                    "historyLow": { "amount": 0.37, "amountInt": 37, "currency": "BRL" },
                    "historyLow_1y": { "amount": 0.37, "amountInt": 37, "currency": "BRL" },
                    "historyLow_3m": { "amount": 0.37, "amountInt": 37, "currency": "BRL" },
                    "flag": "H",
                    "drm": [{ "id": 61, "name": "Steam" }],
                    "platforms": [{ "id": 1, "name": "Windows" }],
                    "timestamp": "2026-08-26T05:47:57+02:00",
                    "expiry": "2026-09-12T04:59:59+02:00",
                    "url": "https://itad.link/018d9386-30e6-719f-8647-a30948d8ab01/?app=tqtwff"
                  }
                },
                {
                  "id": "018d937e-f8d2-7123-9942-85ddaff9efee",
                  "slug": "hyper-simon-x",
                  "title": "Hyper Simon X",
                  "type": "game",
                  "mature": false,
                  "assets": { "boxart": "https://assets.isthereanydeal.com/018d937e-f8d2-7123-9942-85ddaff9efee/boxart.jpg?t=1787433002" },
                  "deal": {
                    "shop": { "id": 50, "name": "Nuuvem" },
                    "price": { "amount": 0.37, "amountInt": 37, "currency": "BRL" },
                    "regular": { "amount": 2.29, "amountInt": 229, "currency": "BRL" },
                    "cut": 84, "voucher": null,
                    "storeLow": { "amount": 0.37, "amountInt": 37, "currency": "BRL" },
                    "historyLow": { "amount": 0.37, "amountInt": 37, "currency": "BRL" },
                    "historyLow_1y": { "amount": 0.37, "amountInt": 37, "currency": "BRL" },
                    "historyLow_3m": { "amount": 0.37, "amountInt": 37, "currency": "BRL" },
                    "flag": "H",
                    "drm": [{ "id": 61, "name": "Steam" }],
                    "platforms": [{ "id": 1, "name": "Windows" }],
                    "timestamp": "2026-08-26T05:47:57+02:00",
                    "expiry": "2026-09-12T04:59:59+02:00",
                    "url": "https://itad.link/018d9386-7979-7079-abd0-2895cfa7fffd/?app=tqtwff"
                  }
                }
              ]
            }
            """;

    // Fixture 2 — POST /lookup/shop/50/id/v1 (real, Nuuvem).
    private static final String FIXTURE_LOOKUP_REQUEST = "[\"018d937e-f8d2-7123-9942-85ddaff9efee\"]";
    private static final String FIXTURE_LOOKUP_RESPONSE =
            "{\"018d937e-f8d2-7123-9942-85ddaff9efee\":[\"14615\"]}";

    // Fixture 3 — corpo de erro real, formato ItadErroResponse
    private static final String FIXTURE_ERRO =
            "{\"status_code\":400,\"reason_phrase\":\"country: Missing data for 'country'\"}";

    // Fixture 3b — corpo de erro plausível pra 429 (rate limit), formato ItadErroResponse
    private static final String FIXTURE_ERRO_429 =
            "{\"status_code\":429,\"reason_phrase\":\"Too Many Requests\"}";

    // Fixture 1b — GET /deals/v2?shops=61 (Steam), item único pra checar o lado Steam
    private static final String FIXTURE_DESCOBERTA_STEAM = """
            {
              "nextOffset": 1,
              "hasMore": false,
              "list": [
                {
                  "id": "018d937e-aaaa-7123-9942-85ddaff9efee",
                  "slug": "some-steam-game",
                  "title": "Some Steam Game",
                  "type": "game",
                  "mature": false,
                  "assets": { "boxart": "https://assets.isthereanydeal.com/018d937e-aaaa-7123-9942-85ddaff9efee/boxart.jpg" },
                  "deal": {
                    "shop": { "id": 61, "name": "Steam" },
                    "price": { "amount": 19.99, "amountInt": 1999, "currency": "BRL" },
                    "regular": { "amount": 39.99, "amountInt": 3999, "currency": "BRL" },
                    "cut": 50, "voucher": null,
                    "storeLow": { "amount": 19.99, "amountInt": 1999, "currency": "BRL" },
                    "flag": "H",
                    "drm": [{ "id": 61, "name": "Steam" }],
                    "platforms": [{ "id": 1, "name": "Windows" }],
                    "timestamp": "2026-08-26T05:47:57+02:00",
                    "expiry": null,
                    "url": "https://itad.link/018d9386-aaaa-719f-8647-a30948d8ab01/?app=tqtwff"
                  }
                }
              ]
            }
            """;

    // Fixture 4 — POST /games/prices/v3
    private static final String FIXTURE_PRECOS = """
            [
              {
                "id": "018d937e-f8d2-7123-9942-85ddaff9efee",
                "historyLow": { "all": null, "y1": null, "m3": null },
                "deals": [
                  {
                    "shop": { "id": 50, "name": "Nuuvem" },
                    "price": { "amount": 0.37, "amountInt": 37, "currency": "BRL" },
                    "regular": { "amount": 2.29, "amountInt": 229, "currency": "BRL" },
                    "cut": 84, "voucher": null,
                    "storeLow": { "amount": 0.37, "amountInt": 37, "currency": "BRL" },
                    "flag": "H",
                    "drm": [{ "id": 61, "name": "Steam" }],
                    "platforms": [{ "id": 1, "name": "Windows" }],
                    "timestamp": "2026-08-26T05:47:57+02:00",
                    "expiry": "2026-09-12T04:59:59+02:00",
                    "url": "https://itad.link/018d9386-7979-7079-abd0-2895cfa7fffd/?app=tqtwff"
                  }
                ]
              }
            ]
            """;

    @Test
    void buscarDescoberta_deveDesserializarRespostaReal() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/deals/v2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_DESCOBERTA)));

        ItadDescobertaResponse resposta = itadClient.buscarDescoberta(List.of(50), 0);

        assertThat(resposta.nextOffset()).isEqualTo(5);
        assertThat(resposta.hasMore()).isTrue();
        assertThat(resposta.list()).hasSize(2);

        ItadJogoDescobertoResponse dlc = resposta.list().get(0);
        ItadJogoDescobertoResponse jogo = resposta.list().get(1);

        assertThat(dlc.type()).isEqualTo("dlc");
        assertThat(jogo.type()).isEqualTo("game");


        assertThat(dlc.deal().expiry()).isNotNull();
        assertThat(jogo.deal().expiry()).isNotNull();
    }

    @Test
    void resolverIdentificadorNativo_deveMapearParaListaDeString() {
        WIREMOCK.stubFor(post(urlPathEqualTo("/lookup/shop/50/id/v1"))
                .withRequestBody(equalToJson(FIXTURE_LOOKUP_REQUEST))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_LOOKUP_RESPONSE)));

        Map<UUID, List<String>> resultado = itadClient.resolverIdentificadorNativo(50, List.of(GID_JOGO));

        assertThat(resultado).containsOnlyKeys(GID_JOGO);
        assertThat(resultado.get(GID_JOGO)).containsExactly("14615");
    }

    @Test
    void buscarPrecos_deveDesserializarSemErro() {
        WIREMOCK.stubFor(post(urlPathEqualTo("/games/prices/v3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_PRECOS)));

        List<ItadPrecoJogoResponse> resultado = itadClient.buscarPrecos(List.of(GID_JOGO));

        assertThat(resultado).hasSize(1);
        ItadPrecoJogoResponse precoJogo = resultado.get(0);
        assertThat(precoJogo.id()).isEqualTo(GID_JOGO);
        assertThat(precoJogo.deals()).hasSize(1);
        assertThat(precoJogo.deals().get(0).expiry()).isNotNull();
    }

    @Test
    void erroHttp_deveLancarItadApiExceptionComStatusEReasonPhraseDoCorpo() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/deals/v2"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_ERRO)));

        assertThatThrownBy(() -> itadClient.buscarDescoberta(List.of(50), 0))
                .isInstanceOf(ItadApiException.class)
                .satisfies(ex -> {
                    ItadApiException itadEx = (ItadApiException) ex;
                    assertThat(itadEx.statusCode()).isEqualTo(400);
                    assertThat(itadEx.reasonPhrase()).isEqualTo("country: Missing data for 'country'");
                });
    }

    @Test
    void erroHttp_comCorpoNaoJson_deveCairParaFallbackComStatusCru() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/deals/v2"))
                .willReturn(aResponse()
                        .withStatus(502)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html><body>Bad Gateway</body></html>")));

        assertThatThrownBy(() -> itadClient.buscarDescoberta(List.of(50), 0))
                .isInstanceOf(ItadApiException.class)
                .satisfies(ex -> {
                    ItadApiException itadEx = (ItadApiException) ex;
                    assertThat(itadEx.statusCode()).isEqualTo(502);
                });
    }

    @Test
    void erroHttp_429_deveLancarItadApiExceptionComStatus429() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/deals/v2"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_ERRO_429)));

        assertThatThrownBy(() -> itadClient.buscarDescoberta(List.of(50), 0))
                .isInstanceOf(ItadApiException.class)
                .satisfies(ex -> {
                    ItadApiException itadEx = (ItadApiException) ex;
                    assertThat(itadEx.statusCode()).isEqualTo(429);
                    assertThat(itadEx.reasonPhrase()).isEqualTo("Too Many Requests");
                });
    }

    @Test
    void buscarDescoberta_deveDesserializarLadoSteam() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/deals/v2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_DESCOBERTA_STEAM)));

        ItadDescobertaResponse resposta = itadClient.buscarDescoberta(List.of(61), 0);

        assertThat(resposta.nextOffset()).isEqualTo(1);
        assertThat(resposta.hasMore()).isFalse();
        assertThat(resposta.list()).hasSize(1);

        ItadJogoDescobertoResponse jogo = resposta.list().get(0);
        assertThat(jogo.type()).isEqualTo("game");
        assertThat(jogo.title()).isEqualTo("Some Steam Game");
        assertThat(jogo.deal().shop().id()).isEqualTo(61);
        assertThat(jogo.deal().shop().name()).isEqualTo("Steam");
        assertThat(jogo.deal().price().amountInt()).isEqualTo(1999);
        assertThat(jogo.deal().regular().amountInt()).isEqualTo(3999);
        assertThat(jogo.deal().cut()).isEqualTo(50);
        assertThat(jogo.deal().expiry()).isNull();
    }
}
