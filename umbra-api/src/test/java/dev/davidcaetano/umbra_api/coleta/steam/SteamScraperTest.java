package dev.davidcaetano.umbra_api.coleta.steam;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.coleta.Cobertura;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.time.ZoneOffset;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SteamScraperTest extends IntegrationTestBase {

    @RegisterExtension
    static WireMockExtension WIREMOCK = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true))
            .build();

    @DynamicPropertySource
    static void configurarBaseUrlSteam(DynamicPropertyRegistry registry) {
        registry.add("steam.steam-api.base-url", WIREMOCK::baseUrl);
    }

    @Autowired
    private SteamScraper steamScraper;

    // ---- fabricantes de fixture — evita repetir o mesmo bloco JSON em cada teste ----

    private static String itemDescoberta(int id, String nome, boolean discounted, long discountExpiration,
                                          String imagemUrl) {
        return """
                {
                  "id": %d,
                  "name": "%s",
                  "discounted": %b,
                  "discount_expiration": %d,
                  "header_image": "%s"
                }
                """.formatted(id, nome, discounted, discountExpiration, imagemUrl);
    }

    private static String descobertaResponse(String... itens) {
        return """
                {
                  "specials": {
                    "id": "cat_specials",
                    "name": "Promoções",
                    "items": [%s]
                  }
                }
                """.formatted(String.join(",", itens));
    }

    private static String detalhesResponse(int appid, String tipo, String currency, int initial, int finalPrice,
                                            int discountPercent) {
        return """
                {
                  "%d": {
                    "success": true,
                    "data": {
                      "type": "%s",
                      "price_overview": {
                        "currency": "%s",
                        "initial": %d,
                        "final": %d,
                        "discount_percent": %d
                      }
                    }
                  }
                }
                """.formatted(appid, tipo, currency, initial, finalPrice, discountPercent);
    }

    private static String detalhesResponseSemSucesso(int appid) {
        return """
                {
                  "%d": {
                    "success": false
                  }
                }
                """.formatted(appid);
    }

    private static String detalhesResponseSemBlocoDePreco(int appid, String tipo) {
        return """
                {
                  "%d": {
                    "success": true,
                    "data": {
                      "type": "%s"
                    }
                  }
                }
                """.formatted(appid, tipo);
    }

    private void stubDescoberta(String corpo) {
        WIREMOCK.stubFor(get(urlPathEqualTo("/featuredcategories"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(corpo)));
    }

    private void stubDetalhes(int appid, String corpo) {
        WIREMOCK.stubFor(get(urlPathEqualTo("/appdetails"))
                .withQueryParam("appids", equalTo(String.valueOf(appid)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(corpo)));
    }

    @Test
    void coletar_deveMontarOfertaColetadaEDescartarItemSemDescontoEDlc() {
        int idSemDesconto = 300;
        int idDlc = 200;
        int idGame = 100;
        long discountExpiration = 1788886800L;

        stubDescoberta(descobertaResponse(
                itemDescoberta(idSemDesconto, "Sem Desconto", false, 0L, "https://img/sem-desconto.jpg"),
                itemDescoberta(idDlc, "DLC Irrelevante", true, discountExpiration, "https://img/dlc.jpg"),
                itemDescoberta(idGame, "Game Valido", true, discountExpiration, "https://img/game.jpg")));

        stubDetalhes(idDlc, detalhesResponse(idDlc, "dlc", "BRL", 1990, 995, 50));
        stubDetalhes(idGame, detalhesResponse(idGame, "game", "BRL", 9990, 4995, 50));

        ResultadoColeta resultado = steamScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(1);
        assertThat(resultado.totalElegivel()).isEqualTo(1);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);
        assertThat(resultado.cobertura()).isEqualTo(Cobertura.amostra());
        assertThat(resultado.fonte()).isEqualTo(OrigemColeta.STEAM_API);
        assertThat(resultado.reconciliacao().declarado()).isNull();
        assertThat(resultado.reconciliacao().brutos()).isEqualTo(3);
        assertThat(resultado.reconciliacao().distintos()).isEqualTo(3);

        OfertaColetada produto = resultado.ofertas().getFirst();
        assertThat(produto.loja()).isEqualTo(CodigoLoja.STEAM);
        assertThat(produto.identificadorLoja()).isEqualTo(String.valueOf(idGame));
        assertThat(produto.tipo()).isEqualTo(TipoProduto.JOGO);
        assertThat(produto.nome()).isEqualTo("Game Valido");
        assertThat(produto.categoria()).isNull();
        assertThat(produto.url()).isEqualTo("https://store.steampowered.com/app/" + idGame + "/");
        assertThat(produto.imagemUrl()).isEqualTo("https://img/game.jpg");
        assertThat(produto.chaveItad()).isNull();
        assertThat(produto.valorCentavos()).isEqualTo(4995);
        assertThat(produto.valorOriginalCentavos()).isEqualTo(9990L);
        assertThat(produto.descontoPct()).isEqualTo((short) 50);
        assertThat(produto.disponivel()).isTrue();
        assertThat(produto.origemColeta()).isEqualTo(OrigemColeta.STEAM_API);
        assertThat(produto.expiry()).isEqualTo(Instant.ofEpochSecond(discountExpiration).atOffset(ZoneOffset.UTC));

        WIREMOCK.verify(0, getRequestedFor(urlPathEqualTo("/appdetails"))
                .withQueryParam("appids", equalTo(String.valueOf(idSemDesconto))));
    }

    @Test
    void coletar_naoDeveChamarAppDetalhesParaItemSemDesconto() {
        int idSemDesconto = 400;

        stubDescoberta(descobertaResponse(
                itemDescoberta(idSemDesconto, "Sem Desconto", false, 0L, "https://img/sem-desconto.jpg")));

        ResultadoColeta resultado = steamScraper.coletar();

        assertThat(resultado.ofertas()).isEmpty();
        assertThat(resultado.totalElegivel()).isEqualTo(0);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);
        WIREMOCK.verify(0, getRequestedFor(urlPathEqualTo("/appdetails"))
                .withQueryParam("appids", equalTo(String.valueOf(idSemDesconto))));
    }

    @Test
    void coletar_deveDescartarItemComSuccessFalseSemLancarErro() {
        int id = 500;

        stubDescoberta(descobertaResponse(
                itemDescoberta(id, "Oferta Invalida", true, 1788886800L, "https://img/invalido.jpg")));
        stubDetalhes(id, detalhesResponseSemSucesso(id));

        ResultadoColeta resultado = steamScraper.coletar();

        assertThat(resultado.ofertas()).isEmpty();
        assertThat(resultado.totalElegivel()).isEqualTo(1);
        assertThat(resultado.totalSemPreco()).isEqualTo(1);
    }

    @Test
    void coletar_deveDescartarItemAusenteDoMapaDeAppDetalhesSemLancarErro() {
        int id = 600;

        stubDescoberta(descobertaResponse(
                itemDescoberta(id, "Sumiu Entre As Duas Chamadas", true, 1788886800L, "https://img/sumido.jpg")));
        stubDetalhes(id, "{}");

        ResultadoColeta resultado = steamScraper.coletar();

        assertThat(resultado.ofertas()).isEmpty();
        assertThat(resultado.totalElegivel()).isEqualTo(1);
        assertThat(resultado.totalSemPreco()).isEqualTo(1);
    }

    @Test
    void coletar_deveDescartarItemForaDaMoedaEsperadaSemLancarErro() {
        int id = 700;

        stubDescoberta(descobertaResponse(
                itemDescoberta(id, "Oferta Fora Da Moeda", true, 1788886800L, "https://img/usd.jpg")));
        stubDetalhes(id, detalhesResponse(id, "game", "USD", 9990, 4995, 50));

        ResultadoColeta resultado = steamScraper.coletar();

        assertThat(resultado.ofertas()).isEmpty();
        assertThat(resultado.totalElegivel()).isEqualTo(0);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);
    }

    @Test
    void coletar_deveDescartarItemSemBlocoDePrecoSemLancarErro() {
        int id = 800;

        stubDescoberta(descobertaResponse(
                itemDescoberta(id, "Sem Bloco De Preco", true, 1788886800L, "https://img/sem-preco.jpg")));
        stubDetalhes(id, detalhesResponseSemBlocoDePreco(id, "game"));

        ResultadoColeta resultado = steamScraper.coletar();

        assertThat(resultado.ofertas()).isEmpty();
        assertThat(resultado.totalElegivel()).isEqualTo(1);
        assertThat(resultado.totalSemPreco()).isEqualTo(1);
    }

    @Test
    void coletar_devePropagarSteamApiExceptionQuandoDescobertaFalha() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/featuredcategories"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> steamScraper.coletar())
                .isInstanceOf(SteamApiException.class)
                .satisfies(ex -> assertThat(((SteamApiException) ex).statusCode()).isEqualTo(500));
    }
}
