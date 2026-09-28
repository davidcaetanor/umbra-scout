package dev.davidcaetano.umbra_api.coleta.kabum;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class KabumScraperTest extends IntegrationTestBase {

    @RegisterExtension
    static WireMockExtension WIREMOCK = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true))
            .build();

    @DynamicPropertySource
    static void configurarBaseUrlKabum(DynamicPropertyRegistry registry) {
        registry.add("kabum.kabum-api.base-url", WIREMOCK::baseUrl);
    }

    @Autowired
    private KabumScraper kabumScraper;

    private static String item(long id, String menu, String title, String price, String priceWithDiscount,
                                int discountPercentage, boolean available, int stock, String productLink,
                                String primeBlock) {
        return """
                {
                  "type": "product",
                  "id": %d,
                  "attributes": {
                    "menu": "%s",
                    "title": "%s",
                    "price": %s,
                    "price_with_discount": %s,
                    "discount_percentage": %d,
                    "available": %b,
                    "stock": %d,
                    "is_marketplace": false,
                    "is_openbox": false,
                    "product_link": "%s",
                    "prime": %s
                  }
                }
                """.formatted(id, menu, title, price, priceWithDiscount, discountPercentage, available, stock,
                productLink, primeBlock);
    }

    private static String itemComFlags(long id, String menu, String title, String price, String priceWithDiscount,
                                        boolean isMarketplace, boolean isOpenbox, String productLink) {
        return """
                {
                  "type": "product",
                  "id": %d,
                  "attributes": {
                    "menu": "%s",
                    "title": "%s",
                    "price": %s,
                    "price_with_discount": %s,
                    "discount_percentage": 0,
                    "available": true,
                    "stock": 1,
                    "is_marketplace": %b,
                    "is_openbox": %b,
                    "product_link": "%s",
                    "prime": null
                  }
                }
                """.formatted(id, menu, title, price, priceWithDiscount, isMarketplace, isOpenbox, productLink);
    }

    private static String itemSemPrecoComDesconto(long id, String menu, String title, String price,
                                                    boolean available, int stock, String productLink) {
        return """
                {
                  "type": "product",
                  "id": %d,
                  "attributes": {
                    "menu": "%s",
                    "title": "%s",
                    "price": %s,
                    "discount_percentage": 0,
                    "available": %b,
                    "stock": %d,
                    "is_marketplace": false,
                    "is_openbox": false,
                    "product_link": "%s",
                    "prime": null
                  }
                }
                """.formatted(id, menu, title, price, available, stock, productLink);
    }

    private static String catalogoResponse(int pageNumber, int totalPagesCount, String... itens) {
        return catalogoResponseComDeclarado(pageNumber, totalPagesCount, 3, itens);
    }

    private static String catalogoResponseComDeclarado(int pageNumber, int totalPagesCount, long totalItemsCount,
                                                         String... itens) {
        return """
                {
                  "meta": {
                    "total_items_count": %d,
                    "total_pages_count": %d,
                    "page": { "number": %d }
                  },
                  "data": [%s]
                }
                """.formatted(totalItemsCount, totalPagesCount, pageNumber, String.join(",", itens));
    }

    private static ListAppender<ILoggingEvent> capturarLogsDoScraper() {
        Logger logger = (Logger) LoggerFactory.getLogger(KabumScraper.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void pararDeCapturar(ListAppender<ILoggingEvent> appender) {
        appender.stop();
        ((Logger) LoggerFactory.getLogger(KabumScraper.class)).detachAppender(appender);
    }

    private void stubPagina(int pageNumber, String corpo) {
        WIREMOCK.stubFor(get(urlPathEqualTo("/catalog/v2/products"))
                .withQueryParam("page_number", equalTo(String.valueOf(pageNumber)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(corpo)));
    }

    // Fixture — resposta real e trimmada da Kabum (capturada da API real em 08/09/2026)
    private static final String FIXTURE_PAGINA_UNICA = """
            {
              "meta": {
                "total_items_count": 3,
                "total_pages_count": 1,
                "page": { "number": 1 }
              },
              "data": [
                {
                  "type": "product",
                  "id": 883976,
                  "attributes": {
                    "menu": "Hardware/Disco Rígido (HD)/Corporativo",
                    "title": "HD WD Gold Enterprise Class HDD 12TB, 7200 RPM, Cache 512MB, CMR, SATA - WD122KRYZ",
                    "price": 7777.77,
                    "price_with_discount": 6999.99,
                    "discount_percentage": 10,
                    "prime": null,
                    "available": true,
                    "stock": 2,
                    "is_marketplace": false,
                    "is_openbox": false,
                    "product_link": "hd-wd-gold-enterprise-class-hdd-12tb-7200-rpm-cache-512mb-cmr-sata-wd122kryz"
                  }
                },
                {
                  "type": "product",
                  "id": 161693,
                  "attributes": {
                    "menu": "Hardware/Placa de vídeo (VGA)/Placa de vídeo AMD",
                    "title": "Placa de Vídeo Afox AMD Radeon R5 230, 1GB DDR3, Low Profile - AFR5230-1024D3L4",
                    "price": 315.5,
                    "price_with_discount": 315.5,
                    "discount_percentage": 0,
                    "prime": null,
                    "available": true,
                    "stock": 1,
                    "is_marketplace": true,
                    "is_openbox": false,
                    "product_link": "placa-de-video-afox-amd-radeon-r5-230-1gb-ddr3-low-profile-afr5230-1024d3l4"
                  }
                },
                {
                  "type": "product",
                  "id": 1059765,
                  "attributes": {
                    "menu": "Hardware/Disco Rígido (HD)/Corporativo/NAS",
                    "title": "HD WD Red Pro, NAS, 26TB, 3.5\\", Cache 512MB, 7200RPM, SATA 6Gb/s - WD260KFGX",
                    "price": 11333.32,
                    "price_with_discount": 10199.99,
                    "discount_percentage": 10,
                    "prime": {
                      "price": 10766.65,
                      "price_with_discount": 9689.99,
                      "discount_percentage": 5,
                      "save": 510,
                      "is_logged_user_exclusive": true
                    },
                    "available": true,
                    "stock": 2,
                    "is_marketplace": false,
                    "is_openbox": false,
                    "product_link": "hd-wd-red-pro-nas-26tb-3-5-cache-512mb-7200rpm-sata-6gb-s-wd260kfgx"
                  }
                }
              ]
            }
            """;

    @Test
    void coletar_deveMapearOsItensDoFixtureRealEDescartarOMarketplace() {
        ListAppender<ILoggingEvent> logs = capturarLogsDoScraper();

        try {
            stubPagina(1, FIXTURE_PAGINA_UNICA);

            ResultadoColeta resultado = kabumScraper.coletar();

            assertThat(resultado.ofertas()).hasSize(2);
            assertThat(resultado.totalElegivel()).isEqualTo(2);
            assertThat(resultado.totalSemPreco()).isEqualTo(0);
            assertThat(kabumScraper.cobertura()).isEqualTo(Cobertura.CENSO);
            assertThat(resultado.fonte()).isEqualTo(OrigemColeta.KABUM_API);
            assertThat(resultado.reconciliacao().declarado()).isEqualTo(3L);
            assertThat(resultado.reconciliacao().brutos()).isEqualTo(3);
            assertThat(resultado.reconciliacao().distintos()).isEqualTo(3);

            assertThat(resultado.ofertas()).noneMatch(o -> o.identificadorLoja().equals("161693"));
            assertThat(logs.list).anySatisfy(evento -> {
                assertThat(evento.getLevel()).isEqualTo(Level.WARN);
                assertThat(evento.getFormattedMessage()).contains("1 itens de marketplace");
            });

            OfertaColetada descontoSimples = resultado.ofertas().get(0);
            assertThat(descontoSimples.loja()).isEqualTo(CodigoLoja.KABUM);
            assertThat(descontoSimples.identificadorLoja()).isEqualTo("883976");
            assertThat(descontoSimples.tipo()).isEqualTo(TipoProduto.HARDWARE);
            assertThat(descontoSimples.categoria()).isEqualTo("HD");
            assertThat(descontoSimples.url())
                    .isEqualTo("https://www.kabum.com.br/produto/883976/hd-wd-gold-enterprise-class-hdd-12tb-7200-rpm-cache-512mb-cmr-sata-wd122kryz");
            assertThat(descontoSimples.imagemUrl()).isNull();
            assertThat(descontoSimples.chaveItad()).isNull();
            assertThat(descontoSimples.valorCentavos()).isEqualTo(699999L);
            assertThat(descontoSimples.valorOriginalCentavos()).isEqualTo(777777L);
            assertThat(descontoSimples.descontoPct()).isEqualTo((short) 10);
            assertThat(descontoSimples.disponivel()).isTrue();
            assertThat(descontoSimples.origemColeta()).isEqualTo(OrigemColeta.KABUM_API);
            assertThat(descontoSimples.expiry()).isNull();

            OfertaColetada comPrime = resultado.ofertas().get(1);
            assertThat(comPrime.identificadorLoja()).isEqualTo("1059765");
            assertThat(comPrime.categoria()).isEqualTo("HD");
            // Usa a price_with_discount publica (10199.99)
            assertThat(comPrime.valorCentavos()).isEqualTo(1019999L);
            assertThat(comPrime.valorOriginalCentavos()).isEqualTo(1133332L);
            assertThat(comPrime.descontoPct()).isEqualTo((short) 10);
        } finally {
            pararDeCapturar(logs);
        }
    }

    @Test
    void coletar_deveDescartarItemOpenboxSemProduzirWarnDeResumo() {
        ListAppender<ILoggingEvent> logs = capturarLogsDoScraper();

        try {
            String itemOpenbox = itemComFlags(999004, "Hardware/SSD/NVMe", "SSD Openbox",
                    "500.00", "450.00", false, true, "ssd-openbox");

            stubPagina(1, catalogoResponse(1, 1, itemOpenbox));

            ResultadoColeta resultado = kabumScraper.coletar();

            assertThat(resultado.ofertas()).isEmpty();
            assertThat(resultado.totalElegivel()).isEqualTo(0);
            assertThat(resultado.totalSemPreco()).isEqualTo(0);
            assertThat(logs.list).noneMatch(evento -> evento.getLevel() == Level.WARN);
        } finally {
            pararDeCapturar(logs);
        }
    }

    @Test
    void coletar_deveDescartarItemComMenuForaDoEscopoMapeadoSemLancarErro() {
        String itemForaDoEscopo = item(999001, "Periféricos/Gabinetes/Gamer", "Gabinete Fora De Escopo",
                "500.00", "500.00", 0, true, 5, "gabinete-fora-de-escopo", "null");

        stubPagina(1, catalogoResponse(1, 1, itemForaDoEscopo));

        ResultadoColeta resultado = kabumScraper.coletar();

        assertThat(resultado.ofertas()).isEmpty();
        assertThat(resultado.totalElegivel()).isEqualTo(0);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);
    }

    @Test
    void coletar_deveManterItemDisponivelFalsoQuandoAvailableMenteSemDescartar() {
        String itemAvailableMentindo = item(999002, "Hardware/SSD/NVMe", "SSD Fantasma",
                "500.00", "0", 0, true, 0, "ssd-fantasma", "null");

        stubPagina(1, catalogoResponse(1, 1, itemAvailableMentindo));

        ResultadoColeta resultado = kabumScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(1);
        assertThat(resultado.totalElegivel()).isEqualTo(1);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);
        assertThat(resultado.ofertas().getFirst().disponivel()).isFalse();
        assertThat(resultado.ofertas().getFirst().categoria()).isEqualTo("SSD");
    }

    @Test
    void coletar_devePaginarCatalogoCompletoEAgregarItensDeTodasAsPaginas() {
        String itemPagina1 = item(1001, "Hardware/Processadores/Intel", "CPU Pagina 1",
                "1000.00", "900.00", 10, true, 3, "cpu-pagina-1", "null");
        String itemPagina2 = item(1002, "Hardware/Fontes/Modular", "Fonte Pagina 2",
                "800.00", "800.00", 0, true, 1, "fonte-pagina-2", "null");

        stubPagina(1, catalogoResponse(1, 2, itemPagina1));
        stubPagina(2, catalogoResponse(2, 2, itemPagina2));

        ResultadoColeta resultado = kabumScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(2);
        assertThat(resultado.ofertas()).extracting(OfertaColetada::identificadorLoja)
                .containsExactlyInAnyOrder("1001", "1002");
        assertThat(resultado.totalElegivel()).isEqualTo(2);
        assertThat(resultado.totalSemPreco()).isEqualTo(0);

        WIREMOCK.verify(getRequestedFor(urlPathEqualTo("/catalog/v2/products"))
                .withQueryParam("page_number", equalTo("1")));
        WIREMOCK.verify(getRequestedFor(urlPathEqualTo("/catalog/v2/products"))
                .withQueryParam("page_number", equalTo("2")));
    }

    @Test
    void coletar_deveMedirDistintosAbaixoDoDeclaradoQuandoPaginacaoRepeteItemEntrePaginas() {
        String itemRepetido = item(3001, "Hardware/Processadores/Intel", "CPU Repetida Entre Paginas",
                "1000.00", "900.00", 10, true, 3, "cpu-repetida", "null");
        String itemPagina1 = item(3002, "Hardware/Fontes/Modular", "Fonte Pagina 1",
                "800.00", "800.00", 0, true, 1, "fonte-pagina-1", "null");
        String itemPagina2 = item(3003, "Hardware/SSD/NVMe", "SSD Pagina 2",
                "500.00", "500.00", 0, true, 5, "ssd-pagina-2", "null");

        stubPagina(1, catalogoResponseComDeclarado(1, 2, 5, itemRepetido, itemPagina1));
        stubPagina(2, catalogoResponseComDeclarado(2, 2, 5, itemRepetido, itemPagina2));

        ResultadoColeta resultado = kabumScraper.coletar();

        assertThat(resultado.reconciliacao().declarado()).isEqualTo(5L);
        assertThat(resultado.reconciliacao().brutos()).isEqualTo(4);
        assertThat(resultado.reconciliacao().distintos()).isEqualTo(3);
        assertThat((long) resultado.reconciliacao().distintos()).isLessThan(resultado.reconciliacao().declarado());
    }

    @Test
    void coletar_naoDeveLancarQuandoUmaPaginaDoMeioVemVaziaEDeveManterOsItensDasOutras() {
        String itemPagina1 = item(4001, "Hardware/Processadores/Intel", "CPU Pagina 1",
                "1000.00", "900.00", 10, true, 3, "cpu-pagina-1", "null");
        String itemPagina3 = item(4002, "Hardware/Fontes/Modular", "Fonte Pagina 3",
                "800.00", "800.00", 0, true, 1, "fonte-pagina-3", "null");

        stubPagina(1, catalogoResponseComDeclarado(1, 3, 3, itemPagina1));
        stubPagina(2, catalogoResponseComDeclarado(2, 3, 3));
        stubPagina(3, catalogoResponseComDeclarado(3, 3, 3, itemPagina3));

        ResultadoColeta resultado = kabumScraper.coletar();

        assertThat(resultado.ofertas()).hasSize(2);
        assertThat(resultado.ofertas()).extracting(OfertaColetada::identificadorLoja)
                .containsExactlyInAnyOrder("4001", "4002");
        assertThat(resultado.totalElegivel()).isEqualTo(2);
        assertThat(resultado.reconciliacao().brutos()).isEqualTo(2);
        assertThat(resultado.reconciliacao().distintos()).isEqualTo(2);
    }

    @Test
    void coletar_deveDescartarItemElegivelSemPrecoSemLancarErro() {
        String itemSemPreco = itemSemPrecoComDesconto(999003, "Hardware/Memória RAM/DDR5", "Memoria Sem Preco",
                "500.00", true, 5, "memoria-sem-preco");

        stubPagina(1, catalogoResponse(1, 1, itemSemPreco));

        ResultadoColeta resultado = kabumScraper.coletar();

        assertThat(resultado.ofertas()).isEmpty();
        assertThat(resultado.totalElegivel()).isEqualTo(1);
        assertThat(resultado.totalSemPreco()).isEqualTo(1);
    }

    @Test
    void coletar_devePropagarKabumApiExceptionQuandoBuscaDaPrimeiraPaginaFalha() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/catalog/v2/products"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> kabumScraper.coletar())
                .isInstanceOf(KabumApiException.class)
                .satisfies(ex -> assertThat(((KabumApiException) ex).statusCode()).isEqualTo(500));
    }
}
