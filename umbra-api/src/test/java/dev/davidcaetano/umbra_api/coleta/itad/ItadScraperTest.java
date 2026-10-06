package dev.davidcaetano.umbra_api.coleta.itad;

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

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ItadScraperTest extends IntegrationTestBase {

    @RegisterExtension
    static WireMockExtension WIREMOCK = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true))
            .build();

    @DynamicPropertySource
    static void configurarBaseUrlItad(DynamicPropertyRegistry registry) {
        registry.add("itad.itad-api.base-url", WIREMOCK::baseUrl);
    }

    @Autowired
    private ItadScraper itadScraper;

    private static final String EXPIRY_NULO = "null";

    // ---- fabricantes de fixture — evita repetir o mesmo bloco JSON em cada teste ----

    private static String jogoDescoberta(UUID id, String titulo, String tipo, String imagemUrl,
                                          int shopId, String shopName, int precoAtual, int precoOriginal,
                                          int desconto, String expiryJson, String dealUrl) {
        return """
                {
                  "id": "%s",
                  "title": "%s",
                  "type": "%s",
                  "assets": { "boxart": "%s" },
                  "deal": {
                    "shop": { "id": %d, "name": "%s" },
                    "price": { "amountInt": %d, "currency": "BRL" },
                    "regular": { "amountInt": %d, "currency": "BRL" },
                    "cut": %d,
                    "timestamp": "2026-09-01T00:00:00Z",
                    "expiry": %s,
                    "url": "%s"
                  }
                }
                """.formatted(id, titulo, tipo, imagemUrl, shopId, shopName, precoAtual, precoOriginal,
                desconto, expiryJson, dealUrl);
    }

    private static String descobertaResponse(boolean hasMore, int nextOffset, String... itens) {
        return """
                {
                  "nextOffset": %d,
                  "hasMore": %b,
                  "list": [%s]
                }
                """.formatted(nextOffset, hasMore, String.join(",", itens));
    }

    private static String lookupComEntrada(UUID gid, String... identificadores) {
        String lista = String.join(",", java.util.Arrays.stream(identificadores)
                .map(i -> "\"" + i + "\"")
                .toArray(String[]::new));
        return "{\"%s\":[%s]}".formatted(gid, lista);
    }

    private static String dealJson(int shopId, String shopName, int precoAtual, int precoOriginal,
                                    int desconto, String dealUrl, String expiryJson) {
        return """
                {
                  "shop": { "id": %d, "name": "%s" },
                  "price": { "amountInt": %d, "currency": "BRL" },
                  "regular": { "amountInt": %d, "currency": "BRL" },
                  "cut": %d,
                  "timestamp": "2026-09-01T00:00:00Z",
                  "expiry": %s,
                  "url": "%s"
                }
                """.formatted(shopId, shopName, precoAtual, precoOriginal, desconto, expiryJson, dealUrl);
    }

    private static String dealJsonComMoedas(int shopId, String shopName, int precoAtual, String precoCurrency,
                                             int precoOriginal, String regularCurrency, int desconto,
                                             String dealUrl, String expiryJson) {
        return """
                {
                  "shop": { "id": %d, "name": "%s" },
                  "price": { "amountInt": %d, "currency": "%s" },
                  "regular": { "amountInt": %d, "currency": "%s" },
                  "cut": %d,
                  "timestamp": "2026-09-01T00:00:00Z",
                  "expiry": %s,
                  "url": "%s"
                }
                """.formatted(shopId, shopName, precoAtual, precoCurrency, precoOriginal, regularCurrency,
                desconto, expiryJson, dealUrl);
    }

    private static String precoJogoJson(UUID gid, String... deals) {
        return """
                {
                  "id": "%s",
                  "deals": [%s]
                }
                """.formatted(gid, String.join(",", deals));
    }

    private static String precosResponse(String... jogos) {
        return "[" + String.join(",", jogos) + "]";
    }

    private void stubDescoberta(String corpo) {
        WIREMOCK.stubFor(get(urlPathEqualTo("/deals/v2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(corpo)));
    }

    private void stubLookup(int shopId, String corpo) {
        WIREMOCK.stubFor(post(urlPathEqualTo("/lookup/shop/" + shopId + "/id/v1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(corpo)));
    }

    private void stubPrecos(String corpo) {
        WIREMOCK.stubFor(post(urlPathEqualTo("/games/prices/v3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(corpo)));
    }

    @Test
    void coletar_deveMontarOfertaColetadaParaSteamENuuvemEDescartarDlc() {
        UUID gidDlc = UUID.fromString("018d0000-0000-7000-8000-000000000003");
        UUID gidNuuvem = UUID.fromString("018d0000-0000-7000-8000-000000000001");
        UUID gidSteam = UUID.fromString("018d0000-0000-7000-8000-000000000002");

        stubDescoberta(descobertaResponse(false, 5,
                jogoDescoberta(gidDlc, "DLC Irrelevante", "dlc", "https://img/dlc.jpg",
                        50, "Nuuvem", 100, 200, 50, EXPIRY_NULO, "https://itad.link/dlc"),
                jogoDescoberta(gidNuuvem, "Jogo Nuuvem", "game", "https://img/nuuvem.jpg",
                        50, "Nuuvem", 1000, 2000, 50, EXPIRY_NULO, "https://itad.link/nuuvem"),
                jogoDescoberta(gidSteam, "Jogo Steam", "game", "https://img/steam.jpg",
                        61, "Steam", 1500, 3000, 50, EXPIRY_NULO, "https://itad.link/steam")));

        stubLookup(50, lookupComEntrada(gidNuuvem, "NUUVEM_ID_1"));
        stubLookup(61, lookupComEntrada(gidSteam, "app/620"));

        stubPrecos(precosResponse(
                precoJogoJson(gidNuuvem, dealJson(50, "Nuuvem", 1000, 2000, 50,
                        "https://itad.link/nuuvem", EXPIRY_NULO)),
                precoJogoJson(gidSteam, dealJson(61, "Steam", 1500, 3000, 50,
                        "https://itad.link/steam", EXPIRY_NULO))));

        ResultadoColeta resultado = itadScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(2);
        assertThat(resultado.ofertas()).noneMatch(p -> p.chaveItad().equals(gidDlc.toString()));
        assertThat(resultado.totalElegivel()).isEqualTo(2);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);
        assertThat(resultado.cobertura()).isEqualTo(Cobertura.amostra());
        assertThat(resultado.fonte()).isEqualTo(OrigemColeta.ITAD_API);
        assertThat(resultado.reconciliacao().declarado()).isNull();
        assertThat(resultado.reconciliacao().brutos()).isEqualTo(3);
        assertThat(resultado.reconciliacao().distintos()).isEqualTo(3);

        OfertaColetada nuuvem = resultado.ofertas().get(0);
        assertThat(nuuvem.loja()).isEqualTo(CodigoLoja.NUUVEM);
        assertThat(nuuvem.identificadorLoja()).isEqualTo("NUUVEM_ID_1");
        assertThat(nuuvem.tipo()).isEqualTo(TipoProduto.JOGO);
        assertThat(nuuvem.nome()).isEqualTo("Jogo Nuuvem");
        assertThat(nuuvem.categoria()).isNull();
        assertThat(nuuvem.url()).isEqualTo("https://itad.link/nuuvem");
        assertThat(nuuvem.imagemUrl()).isEqualTo("https://img/nuuvem.jpg");
        assertThat(nuuvem.chaveItad()).isEqualTo(gidNuuvem.toString());
        assertThat(nuuvem.valorCentavos()).isEqualTo(1000);
        assertThat(nuuvem.valorOriginalCentavos()).isEqualTo(2000L);
        assertThat(nuuvem.descontoPct()).isEqualTo((short) 50);
        assertThat(nuuvem.disponivel()).isTrue();
        assertThat(nuuvem.origemColeta()).isEqualTo(OrigemColeta.ITAD_API);
        assertThat(nuuvem.expiry()).isNull();

        OfertaColetada steam = resultado.ofertas().get(1);
        assertThat(steam.loja()).isEqualTo(CodigoLoja.STEAM);
        assertThat(steam.identificadorLoja()).isEqualTo("620");
        assertThat(steam.nome()).isEqualTo("Jogo Steam");
        assertThat(steam.imagemUrl()).isEqualTo("https://img/steam.jpg");
        assertThat(steam.chaveItad()).isEqualTo(gidSteam.toString());
        assertThat(steam.valorCentavos()).isEqualTo(1500);
        assertThat(steam.valorOriginalCentavos()).isEqualTo(3000L);
        assertThat(steam.descontoPct()).isEqualTo((short) 50);
    }

    @Test
    void coletar_deveIgnorarJogoSemOfertaSemLancarErro() {
        UUID gid = UUID.fromString("018d0000-0000-7000-8000-000000000004");

        stubDescoberta(descobertaResponse(false, 1,
                jogoDescoberta(gid, "Jogo Sem Oferta", "game", "https://img/x.jpg",
                        50, "Nuuvem", 1000, 2000, 50, EXPIRY_NULO, "https://itad.link/x")));

        stubLookup(50, lookupComEntrada(gid, "999"));

        // /games/prices/v3 não traz nenhum jogo — a promoção acabou entre as duas chamadas.
        stubPrecos(precosResponse());

        ResultadoColeta resultado = itadScraper.coletar();

        assertThat(resultado.ofertas()).isEmpty();
        assertThat(resultado.totalElegivel()).isEqualTo(0);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);
    }

    @Test
    void coletar_deveDescartarItemComIdentificadorNativoAusente() {
        UUID gidSemIdentificador = UUID.fromString("018d0000-0000-7000-8000-000000000005");
        UUID gidComIdentificador = UUID.fromString("018d0000-0000-7000-8000-000000000006");

        stubDescoberta(descobertaResponse(false, 1,
                jogoDescoberta(gidSemIdentificador, "Jogo Sem Identificador", "game", "https://img/a.jpg",
                        50, "Nuuvem", 1000, 2000, 50, EXPIRY_NULO, "https://itad.link/a"),
                jogoDescoberta(gidComIdentificador, "Jogo Com Identificador", "game", "https://img/b.jpg",
                        61, "Steam", 1500, 3000, 50, EXPIRY_NULO, "https://itad.link/b")));

        stubLookup(50, "{}");
        stubLookup(61, lookupComEntrada(gidComIdentificador, "700"));

        stubPrecos(precosResponse(
                precoJogoJson(gidSemIdentificador, dealJson(50, "Nuuvem", 1000, 2000, 50,
                        "https://itad.link/a", EXPIRY_NULO)),
                precoJogoJson(gidComIdentificador, dealJson(61, "Steam", 1500, 3000, 50,
                        "https://itad.link/b", EXPIRY_NULO))));

        ResultadoColeta resultado = itadScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(1);
        assertThat(resultado.ofertas().get(0).chaveItad()).isEqualTo(gidComIdentificador.toString());
        assertThat(resultado.ofertas().get(0).identificadorLoja()).isEqualTo("700");
        assertThat(resultado.totalElegivel()).isEqualTo(2);
        assertThat(resultado.totalSemPreco()).isEqualTo(1);
    }

    @Test
    void coletar_devePaginarDescobertaAteHasMoreFalso() {
        UUID gidPagina1 = UUID.fromString("018d0000-0000-7000-8000-000000000007");
        UUID gidPagina2 = UUID.fromString("018d0000-0000-7000-8000-000000000008");

        WIREMOCK.stubFor(get(urlPathEqualTo("/deals/v2"))
                .withQueryParam("offset", equalTo("0"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(descobertaResponse(true, 5,
                                jogoDescoberta(gidPagina1, "Jogo Pagina 1", "game", "https://img/p1.jpg",
                                        50, "Nuuvem", 1000, 2000, 50, EXPIRY_NULO, "https://itad.link/p1")))));

        WIREMOCK.stubFor(get(urlPathEqualTo("/deals/v2"))
                .withQueryParam("offset", equalTo("5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(descobertaResponse(false, 5,
                                jogoDescoberta(gidPagina2, "Jogo Pagina 2", "game", "https://img/p2.jpg",
                                        61, "Steam", 1500, 3000, 50, EXPIRY_NULO, "https://itad.link/p2")))));

        stubLookup(50, lookupComEntrada(gidPagina1, "111"));
        stubLookup(61, lookupComEntrada(gidPagina2, "222"));

        stubPrecos(precosResponse(
                precoJogoJson(gidPagina1, dealJson(50, "Nuuvem", 1000, 2000, 50,
                        "https://itad.link/p1", EXPIRY_NULO)),
                precoJogoJson(gidPagina2, dealJson(61, "Steam", 1500, 3000, 50,
                        "https://itad.link/p2", EXPIRY_NULO))));

        ResultadoColeta resultado = itadScraper.coletar();

        WIREMOCK.verify(2, getRequestedFor(urlPathEqualTo("/deals/v2")));

        assertThat(resultado.ofertas()).hasSize(2);
        assertThat(resultado.ofertas()).extracting(OfertaColetada::identificadorLoja)
                .containsExactlyInAnyOrder("111", "222");
        assertThat(resultado.totalElegivel()).isEqualTo(2);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);
    }

    @Test
    void coletar_deveDescartarOfertaForaDaMoedaEsperada() {
        UUID gidBrl = UUID.fromString("018d0000-0000-7000-8000-000000000009");
        UUID gidUsd = UUID.fromString("018d0000-0000-7000-8000-00000000000a");

        stubDescoberta(descobertaResponse(false, 1,
                jogoDescoberta(gidBrl, "Jogo BRL", "game", "https://img/brl.jpg",
                        50, "Nuuvem", 1000, 2000, 50, EXPIRY_NULO, "https://itad.link/brl"),
                jogoDescoberta(gidUsd, "Jogo USD", "game", "https://img/usd.jpg",
                        61, "Steam", 1500, 3000, 50, EXPIRY_NULO, "https://itad.link/usd")));

        stubLookup(50, lookupComEntrada(gidBrl, "800"));
        stubLookup(61, lookupComEntrada(gidUsd, "900"));

        stubPrecos(precosResponse(
                precoJogoJson(gidBrl, dealJson(50, "Nuuvem", 1000, 2000, 50,
                        "https://itad.link/brl", EXPIRY_NULO)),
                precoJogoJson(gidUsd, dealJsonComMoedas(61, "Steam", 1500, "USD", 3000, "USD", 50,
                        "https://itad.link/usd", EXPIRY_NULO))));

        ResultadoColeta resultado = itadScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(1);
        assertThat(resultado.ofertas().get(0).chaveItad()).isEqualTo(gidBrl.toString());
        assertThat(resultado.totalElegivel()).isEqualTo(1);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);
    }

    @Test
    void coletar_deveDescartarOfertaDeShopDesconhecidoSemLancarErro() {
        UUID gidConhecido = UUID.fromString("018d0000-0000-7000-8000-00000000000b");
        UUID gidDesconhecido = UUID.fromString("018d0000-0000-7000-8000-00000000000c");

        stubDescoberta(descobertaResponse(false, 1,
                jogoDescoberta(gidConhecido, "Jogo Shop Conhecido", "game", "https://img/conhecido.jpg",
                        61, "Steam", 1500, 3000, 50, EXPIRY_NULO, "https://itad.link/conhecido"),
                jogoDescoberta(gidDesconhecido, "Jogo Shop Desconhecido", "game", "https://img/desconhecido.jpg",
                        61, "Steam", 1000, 2000, 50, EXPIRY_NULO, "https://itad.link/desconhecido")));

        stubLookup(61, lookupComEntrada(gidConhecido, "610"));

        stubPrecos(precosResponse(
                precoJogoJson(gidConhecido, dealJson(61, "Steam", 1500, 3000, 50,
                        "https://itad.link/conhecido", EXPIRY_NULO)),
                precoJogoJson(gidDesconhecido, dealJson(99, "LojaDesconhecida", 1000, 2000, 50,
                        "https://itad.link/desconhecido", EXPIRY_NULO))));

        ResultadoColeta resultado = itadScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(1);
        assertThat(resultado.ofertas().get(0).chaveItad()).isEqualTo(gidConhecido.toString());
        assertThat(resultado.totalElegivel()).isEqualTo(2);
        assertThat(resultado.totalSemPreco()).isEqualTo(1);
    }

    @Test
    void coletar_deveColetarOfertaDeLojaOndeOJogoNaoTeveDesconto() {
        UUID gid = UUID.fromString("018d0000-0000-7000-8000-00000000000d");

        stubDescoberta(descobertaResponse(false, 1,
                jogoDescoberta(gid, "Jogo Multi Loja", "game", "https://img/multi.jpg",
                        61, "Steam", 1500, 3000, 50, EXPIRY_NULO, "https://itad.link/multi-steam")));

        stubLookup(61, lookupComEntrada(gid, "610"));
        stubLookup(50, lookupComEntrada(gid, "NUUVEM_610"));

        stubPrecos(precosResponse(
                precoJogoJson(gid,
                        dealJson(61, "Steam", 1500, 3000, 50, "https://itad.link/multi-steam", EXPIRY_NULO),
                        dealJson(50, "Nuuvem", 2900, 2900, 0, "https://itad.link/multi-nuuvem", EXPIRY_NULO))));

        ResultadoColeta resultado = itadScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(2);
        assertThat(resultado.ofertas()).extracting(OfertaColetada::loja)
                .containsExactlyInAnyOrder(CodigoLoja.STEAM, CodigoLoja.NUUVEM);
        assertThat(resultado.totalElegivel()).isEqualTo(2);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);
    }

    @Test
    void coletar_deveEscolherIdentificadorNativoDeFormaDeterministicaQuandoHouverMaisDeUm() {
        UUID gid = UUID.fromString("018d0000-0000-7000-8000-00000000000e");

        stubDescoberta(descobertaResponse(false, 1,
                jogoDescoberta(gid, "Jogo Multi Id", "game", "https://img/multiid.jpg",
                        61, "Steam", 1500, 3000, 50, EXPIRY_NULO, "https://itad.link/multiid")));

        stubLookup(61, lookupComEntrada(gid, "zzz-id", "aaa-id"));

        stubPrecos(precosResponse(
                precoJogoJson(gid, dealJson(61, "Steam", 1500, 3000, 50,
                        "https://itad.link/multiid", EXPIRY_NULO))));

        ResultadoColeta resultado = itadScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(1);
        assertThat(resultado.ofertas().get(0).identificadorLoja()).isEqualTo("aaa-id");
    }

    @Test
    void coletar_deveGravarIdentificadorDaSteamDeOutroTipoSemRemoverPrefixo() {
        UUID gid = UUID.fromString("018d0000-0000-7000-8000-000000000012");

        stubDescoberta(descobertaResponse(false, 1,
                jogoDescoberta(gid, "Jogo Pacote Steam", "game", "https://img/sub.jpg",
                        61, "Steam", 1500, 3000, 50, EXPIRY_NULO, "https://itad.link/sub")));

        stubLookup(61, lookupComEntrada(gid, "sub/123"));

        stubPrecos(precosResponse(
                precoJogoJson(gid, dealJson(61, "Steam", 1500, 3000, 50,
                        "https://itad.link/sub", EXPIRY_NULO))));

        ResultadoColeta resultado = itadScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(1);
        assertThat(resultado.ofertas().get(0).identificadorLoja()).isEqualTo("sub/123");
    }

    @Test
    void coletar_deveColetarJogoGratuitoComValorZeroEDisponivel() {
        UUID gid = UUID.fromString("018d0000-0000-7000-8000-000000000013");

        stubDescoberta(descobertaResponse(false, 1,
                jogoDescoberta(gid, "Jogo Gratuito", "game", "https://img/gratuito.jpg",
                        61, "Steam", 0, 0, 0, EXPIRY_NULO, "https://itad.link/gratuito")));

        stubLookup(61, lookupComEntrada(gid, "app/730"));

        stubPrecos(precosResponse(
                precoJogoJson(gid, dealJson(61, "Steam", 0, 0, 0,
                        "https://itad.link/gratuito", EXPIRY_NULO))));

        ResultadoColeta resultado = itadScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(1);
        assertThat(resultado.totalElegivel()).isEqualTo(1);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);

        OfertaColetada gratuito = resultado.ofertas().get(0);
        assertThat(gratuito.valorCentavos()).isEqualTo(0);
        assertThat(gratuito.valorOriginalCentavos()).isEqualTo(0L);
        assertThat(gratuito.disponivel()).isTrue();
    }

    @Test
    void coletar_deveColetarPromocaoDeCemPorCentoComValorOriginalDaFonte() {
        UUID gid = UUID.fromString("018d0000-0000-7000-8000-000000000014");

        stubDescoberta(descobertaResponse(false, 1,
                jogoDescoberta(gid, "Jogo Em Promocao Total", "game", "https://img/promo.jpg",
                        61, "Steam", 0, 5990, 100, EXPIRY_NULO, "https://itad.link/promo")));

        stubLookup(61, lookupComEntrada(gid, "app/4000"));

        stubPrecos(precosResponse(
                precoJogoJson(gid, dealJson(61, "Steam", 0, 5990, 100,
                        "https://itad.link/promo", EXPIRY_NULO))));

        ResultadoColeta resultado = itadScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(1);

        OfertaColetada promocao = resultado.ofertas().get(0);
        assertThat(promocao.valorCentavos()).isEqualTo(0);
        assertThat(promocao.valorOriginalCentavos()).isEqualTo(5990L);
        assertThat(promocao.descontoPct()).isEqualTo((short) 100);
    }

    @Test
    void coletar_deveColetarOfertaDeLojaNovaDoItad() {
        UUID gid = UUID.fromString("018d0000-0000-7000-8000-000000000011");

        stubDescoberta(descobertaResponse(false, 1,
                jogoDescoberta(gid, "Jogo GOG", "game", "https://img/gog.jpg",
                        35, "GOG", 1500, 3000, 50, EXPIRY_NULO, "https://itad.link/gog")));

        stubLookup(35, lookupComEntrada(gid, "gog_jogo_gog"));

        stubPrecos(precosResponse(
                precoJogoJson(gid, dealJson(35, "GOG", 1500, 3000, 50,
                        "https://itad.link/gog", EXPIRY_NULO))));

        ResultadoColeta resultado = itadScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(1);
        assertThat(resultado.ofertas().get(0).loja()).isEqualTo(CodigoLoja.GOG);
        assertThat(resultado.totalElegivel()).isEqualTo(1);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);
    }

    @Test
    void coletar_naoDevePedirIdentificadorParaShopForaDasLojasSuportadas() {
        UUID gidConhecido = UUID.fromString("018d0000-0000-7000-8000-00000000000f");
        UUID gidDesconhecido = UUID.fromString("018d0000-0000-7000-8000-000000000010");

        stubDescoberta(descobertaResponse(false, 1,
                jogoDescoberta(gidConhecido, "Jogo Shop Conhecido", "game", "https://img/conhecido.jpg",
                        61, "Steam", 1500, 3000, 50, EXPIRY_NULO, "https://itad.link/conhecido"),
                jogoDescoberta(gidDesconhecido, "Jogo Shop Desconhecido", "game", "https://img/desconhecido.jpg",
                        61, "Steam", 1000, 2000, 50, EXPIRY_NULO, "https://itad.link/desconhecido")));

        stubLookup(61, lookupComEntrada(gidConhecido, "610"));

        stubPrecos(precosResponse(
                precoJogoJson(gidConhecido, dealJson(61, "Steam", 1500, 3000, 50,
                        "https://itad.link/conhecido", EXPIRY_NULO)),
                precoJogoJson(gidDesconhecido, dealJson(99, "LojaDesconhecida", 1000, 2000, 50,
                        "https://itad.link/desconhecido", EXPIRY_NULO))));

        ResultadoColeta resultado = itadScraper.coletar();

        WIREMOCK.verify(0, postRequestedFor(urlPathEqualTo("/lookup/shop/99/id/v1")));

        assertThat(resultado.ofertas()).hasSize(1);
        assertThat(resultado.ofertas().get(0).chaveItad()).isEqualTo(gidConhecido.toString());
        assertThat(resultado.totalElegivel()).isEqualTo(2);
        assertThat(resultado.totalSemPreco()).isEqualTo(1);
    }
}
