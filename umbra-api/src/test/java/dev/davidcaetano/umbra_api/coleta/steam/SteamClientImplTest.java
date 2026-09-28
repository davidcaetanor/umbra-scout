package dev.davidcaetano.umbra_api.coleta.steam;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamAppDetalhesResponse;
import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamDescobertaResponse;
import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamJogoDescobertoResponse;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SteamClientImplTest extends IntegrationTestBase {

    @RegisterExtension
    static WireMockExtension WIREMOCK = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true))
            .build();

    @DynamicPropertySource
    static void configurarBaseUrlSteam(DynamicPropertyRegistry registry) {
        registry.add("steam.steam-api.base-url", WIREMOCK::baseUrl);
    }

    @Autowired
    private SteamClient steamClient;

    private static final int APPID_RDR2 = 1174180;

    // Fixture 1 — GET /featuredcategories?cc=br&l=portuguese (real, capturada da API real em 08/09/2026).
    // Mantido apenas o bloco "specials", os demais (spotlights, daily deal, coming soon etc.) não são mapeados por
    // SteamDescobertaResponse e são descartados pelo @JsonIgnoreProperties(ignoreUnknown = true).
    private static final String FIXTURE_DESCOBERTA = """
            {
              "specials": {
                "id": "cat_specials",
                "name": "Promo\\u00e7\\u00f5es",
                "items": [
                  {
                    "id": 1174180,
                    "type": 0,
                    "name": "Red Dead Redemption 2",
                    "discounted": true,
                    "discount_percent": 75,
                    "original_price": 29990,
                    "final_price": 7497,
                    "currency": "BRL",
                    "large_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1174180/capsule_616x353.jpg?t=1759502961",
                    "small_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1174180/capsule_184x69.jpg?t=1759502961",
                    "windows_available": true,
                    "mac_available": false,
                    "linux_available": false,
                    "streamingvideo_available": false,
                    "discount_expiration": 1788886800,
                    "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1174180/header.jpg?t=1759502961"
                  },
                  {
                    "id": 2406770,
                    "type": 0,
                    "name": "Bodycam",
                    "discounted": true,
                    "discount_percent": 20,
                    "original_price": 10199,
                    "final_price": 8159,
                    "currency": "BRL",
                    "large_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/2406770/49a2dcf45baf6adb4655b5011b7e0f1e1913e130/capsule_616x353_alt_assets_0_portuguese.jpg?t=1788548796",
                    "small_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/2406770/2bc46dcf375a61a0a5626bc96aaf09e32436e8cd/capsule_184x69.jpg?t=1788548796",
                    "windows_available": true,
                    "mac_available": false,
                    "linux_available": false,
                    "streamingvideo_available": false,
                    "discount_expiration": 1789059600,
                    "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/2406770/98a9d087d958e0dbb292a7e2c7898653c9b411ba/header_alt_assets_0_portuguese.jpg?t=1788548796",
                    "controller_support": "full"
                  },
                  {
                    "id": 3240220,
                    "type": 0,
                    "name": "Grand Theft Auto V Enhanced",
                    "discounted": true,
                    "discount_percent": 56,
                    "original_price": 22490,
                    "final_price": 9895,
                    "currency": "BRL",
                    "large_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/3240220/4c8d7ce5142a528bdac68c093bd1bcc720e2baee/capsule_616x353.jpg?t=1781187782",
                    "small_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/3240220/a318bd9affe8eee32984b18794b273c256e9b2d6/capsule_184x69.jpg?t=1781187782",
                    "windows_available": true,
                    "mac_available": false,
                    "linux_available": false,
                    "streamingvideo_available": false,
                    "discount_expiration": 1788800400,
                    "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/3240220/header.jpg?t=1781187782"
                  },
                  {
                    "id": 2183900,
                    "type": 0,
                    "name": "Warhammer 40,000: Space Marine 2",
                    "discounted": true,
                    "discount_percent": 75,
                    "original_price": 19990,
                    "final_price": 4997,
                    "currency": "BRL",
                    "large_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/2183900/db611b4424c2b7799c943c0424ca82227300fd69/capsule_616x353.jpg?t=1787837240",
                    "small_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/2183900/capsule_184x69.jpg?t=1787837240",
                    "windows_available": true,
                    "mac_available": false,
                    "linux_available": false,
                    "streamingvideo_available": false,
                    "discount_expiration": 1789405200,
                    "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/2183900/header.jpg?t=1787837240",
                    "controller_support": "full"
                  },
                  {
                    "id": 108600,
                    "type": 0,
                    "name": "Project Zomboid",
                    "discounted": true,
                    "discount_percent": 33,
                    "original_price": 8399,
                    "final_price": 5627,
                    "currency": "BRL",
                    "large_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/108600/capsule_616x353.jpg?t=1787740093",
                    "small_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/108600/capsule_184x69.jpg?t=1787740093",
                    "windows_available": true,
                    "mac_available": true,
                    "linux_available": true,
                    "streamingvideo_available": false,
                    "discount_expiration": 1788800400,
                    "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/108600/header.jpg?t=1787740093",
                    "headline": "Acesso Antecipado j\\u00e1 dispon\\u00edvel"
                  },
                  {
                    "id": 1326470,
                    "type": 0,
                    "name": "Sons Of The Forest",
                    "discounted": true,
                    "discount_percent": 70,
                    "original_price": 8899,
                    "final_price": 2669,
                    "currency": "BRL",
                    "large_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1326470/capsule_616x353.jpg?t=1708624856",
                    "small_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1326470/capsule_184x69.jpg?t=1708624856",
                    "windows_available": true,
                    "mac_available": false,
                    "linux_available": false,
                    "streamingvideo_available": false,
                    "discount_expiration": 1788800400,
                    "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1326470/header.jpg?t=1708624856",
                    "controller_support": "full"
                  },
                  {
                    "id": 1623730,
                    "type": 0,
                    "name": "Palworld",
                    "discounted": true,
                    "discount_percent": 30,
                    "original_price": 8899,
                    "final_price": 6229,
                    "currency": "BRL",
                    "large_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1623730/57df7aed61d65c9012a11d58e812aff275b7daed/capsule_616x353.jpg?t=1784714419",
                    "small_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1623730/0703d5a0658c7aedaa3be4391d6f5cccc1bb6d7a/capsule_184x69.jpg?t=1784714419",
                    "windows_available": true,
                    "mac_available": false,
                    "linux_available": false,
                    "streamingvideo_available": false,
                    "discount_expiration": 1788886800,
                    "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1623730/6912f19c43a95ff5fe514eedd35e68bf12335459/header.jpg?t=1784714419",
                    "controller_support": "full"
                  },
                  {
                    "id": 4656000,
                    "type": 0,
                    "name": "BOMBANANA!",
                    "discounted": true,
                    "discount_percent": 38,
                    "original_price": 1999,
                    "final_price": 1239,
                    "currency": "BRL",
                    "large_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/4656000/5c8c9fb42a56e6fd25f2877053158fd207ffb028/capsule_616x353.jpg?t=1788351950",
                    "small_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/4656000/4b28cb7e3572e7ab0ffcd6a53030b1a719ff3fff/capsule_184x69.jpg?t=1788351950",
                    "windows_available": true,
                    "mac_available": true,
                    "linux_available": false,
                    "streamingvideo_available": false,
                    "discount_expiration": 1788973248,
                    "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/4656000/99c086faba625a8d3bc459bab444087d257ccbe1/header.jpg?t=1788351950",
                    "controller_support": "full"
                  },
                  {
                    "id": 3321460,
                    "type": 0,
                    "name": "Crimson Desert Enhanced",
                    "discounted": true,
                    "discount_percent": 20,
                    "original_price": 34999,
                    "final_price": 27999,
                    "currency": "BRL",
                    "large_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/3321460/c0f0866161be038cc6607824505b0cb51cd12cf0/capsule_616x353.jpg?t=1788450739",
                    "small_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/3321460/9fef3b5648defc30432d4b3221296350354b8742/capsule_184x69.jpg?t=1788450739",
                    "windows_available": true,
                    "mac_available": true,
                    "linux_available": false,
                    "streamingvideo_available": false,
                    "discount_expiration": 1788966000,
                    "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/3321460/236f3814be7a97d86831800691b6096d449222a8/header.jpg?t=1788450739",
                    "controller_support": "full"
                  },
                  {
                    "id": 105600,
                    "type": 0,
                    "name": "Terraria",
                    "discounted": true,
                    "discount_percent": 50,
                    "original_price": 3299,
                    "final_price": 1649,
                    "currency": "BRL",
                    "large_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/105600/capsule_616x353.jpg?t=1769844435",
                    "small_capsule_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/105600/capsule_184x69.jpg?t=1769844435",
                    "windows_available": true,
                    "mac_available": true,
                    "linux_available": true,
                    "streamingvideo_available": false,
                    "discount_expiration": 1788800400,
                    "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/105600/header.jpg?t=1769844435",
                    "headline": "Nova atualiza\\u00e7\\u00e3o",
                    "controller_support": "full"
                  }
                ]
              }
            }
            """;

    // Fixture 2 — GET /appdetails?appids=1174180&cc=br
    private static final String FIXTURE_APPDETALHES = """
            {
              "1174180": {
                "success": true,
                "data": {
                  "type": "game",
                  "name": "Red Dead Redemption 2",
                  "steam_appid": 1174180,
                  "required_age": 0,
                  "is_free": false,
                  "short_description": "Arthur Morgan e a gangue Van der Linde s\\u00e3o for\\u00e7ados a fugir.",
                  "header_image": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1174180/header.jpg?t=1759502961",
                  "developers": ["Rockstar Games"],
                  "publishers": ["Rockstar Games"],
                  "price_overview": {
                    "currency": "BRL",
                    "initial": 29990,
                    "final": 7497,
                    "discount_percent": 75,
                    "initial_formatted": "R$ 299,90",
                    "final_formatted": "R$ 74,97"
                  },
                  "platforms": {
                    "windows": true,
                    "mac": false,
                    "linux": false
                  },
                  "metacritic": {
                    "score": 93,
                    "url": "https://www.metacritic.com/game/pc/red-dead-redemption-2?ftag=MCD-06-10aaa1f"
                  },
                  "genres": [
                    { "id": "1", "description": "A\\u00e7\\u00e3o" },
                    { "id": "25", "description": "Aventura" }
                  ],
                  "recommendations": { "total": 843070 },
                  "release_date": { "coming_soon": false, "date": "5/dez./2019" },
                  "support_info": { "url": "https://support.rockstargames.com/", "email": "" },
                  "background": "https://store.akamai.steamstatic.com/images/storepagebackground/app/1174180?t=1759502961"
                }
              }
            }
            """;

    @Test
    void buscarDescoberta_deveDesserializarRespostaReal() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/featuredcategories"))
                .withQueryParam("cc", equalTo("br"))
                .withQueryParam("l", equalTo("portuguese"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_DESCOBERTA)));

        SteamDescobertaResponse resposta = steamClient.buscarDescoberta();

        assertThat(resposta.specials()).isNotNull();
        assertThat(resposta.specials().items()).hasSize(10);

        SteamJogoDescobertoResponse primeiro = resposta.specials().items().get(0);
        assertThat(primeiro.id()).isEqualTo(1174180);
        assertThat(primeiro.name()).isEqualTo("Red Dead Redemption 2");
        assertThat(primeiro.discounted()).isTrue();
        assertThat(primeiro.discountExpiration()).isEqualTo(1788886800L);
        assertThat(primeiro.headerImage())
                .isEqualTo("https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1174180/header.jpg?t=1759502961");
    }

    @Test
    void buscarDetalhes_deveDesserializarRespostaReal() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/appdetails"))
                .withQueryParam("appids", equalTo(String.valueOf(APPID_RDR2)))
                .withQueryParam("cc", equalTo("br"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_APPDETALHES)));

        SteamAppDetalhesResponse resposta = steamClient.buscarDetalhes(APPID_RDR2);

        assertThat(resposta.success()).isTrue();
        assertThat(resposta.data().type()).isEqualTo("game");
        assertThat(resposta.data().overview().currency()).isEqualTo("BRL");
        assertThat(resposta.data().overview().initial()).isEqualTo(29990);
        assertThat(resposta.data().overview().finalPrice()).isEqualTo(7497);
        assertThat(resposta.data().overview().discountPercent()).isEqualTo(75);
    }

    @Test
    void buscarDetalhes_comAppidAusenteDoMapa_deveDevolverNullSemLancarExcecao() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/appdetails"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        SteamAppDetalhesResponse resposta = steamClient.buscarDetalhes(APPID_RDR2);

        assertThat(resposta).isNull();
    }

    @Test
    void erroHttp_429_deveLancarSteamApiExceptionComStatus429() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/featuredcategories"))
                .willReturn(aResponse().withStatus(429)));

        assertThatThrownBy(steamClient::buscarDescoberta)
                .isInstanceOf(SteamApiException.class)
                .satisfies(ex -> assertThat(((SteamApiException) ex).statusCode()).isEqualTo(429));
    }

    @Test
    void erroHttp_500_deveLancarSteamApiExceptionComStatus500() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/featuredcategories"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(steamClient::buscarDescoberta)
                .isInstanceOf(SteamApiException.class)
                .satisfies(ex -> assertThat(((SteamApiException) ex).statusCode()).isEqualTo(500));
    }

    @Test
    void corpoInvalidoComStatus200_deveLancarSteamApiExceptionComStatusSentinela() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/featuredcategories"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("isso nao eh json")));

        assertThatThrownBy(steamClient::buscarDescoberta)
                .isInstanceOf(SteamApiException.class)
                .satisfies(ex -> assertThat(((SteamApiException) ex).statusCode()).isEqualTo(0));
    }
}
