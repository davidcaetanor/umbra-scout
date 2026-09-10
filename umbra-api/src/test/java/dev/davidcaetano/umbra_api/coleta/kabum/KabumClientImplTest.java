package dev.davidcaetano.umbra_api.coleta.kabum;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.davidcaetano.umbra_api.coleta.kabum.dto.response.KabumCatalogoResponse;
import dev.davidcaetano.umbra_api.coleta.kabum.dto.response.KabumProdutoResponse;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class KabumClientImplTest extends IntegrationTestBase {

    @RegisterExtension
    static WireMockExtension WIREMOCK = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true))
            .build();

    @DynamicPropertySource
    static void configurarBaseUrlKabum(DynamicPropertyRegistry registry) {
        registry.add("kabum.kabum-api.base-url", WIREMOCK::baseUrl);
    }

    @Autowired
    private KabumClient kabumClient;

    // base64('{"category":["Hardware"]}') — único facet aceito por meta.filters.string_filters (KABUM-HARDWARE.md §2)
    private static final String FACET_FILTERS_HARDWARE = "eyJjYXRlZ29yeSI6WyJIYXJkd2FyZSJdfQ==";

    // Fixture — GET /catalog/v2/products?facet_filters=...&page_number=1&page_size=100
    private static final String FIXTURE_CATALOGO_HARDWARE = """
            {
              "meta": {
                "total_items_count": 8155,
                "total_pages_count": 82,
                "page": {
                  "cursor": "",
                  "number": 1,
                  "size": 100,
                  "is_current_page": true
                }
              },
              "links": {
                "redirect": null,
                "first": "/catalog/v2/products?facet_filters=eyJjYXRlZ29yeSI6WyJIYXJkd2FyZSJdfQ==&page_cursor=Tk9UIElTIFVTRQ==&page_number=1&page_size=100",
                "self": "/catalog/v2/products?facet_filters=eyJjYXRlZ29yeSI6WyJIYXJkd2FyZSJdfQ==&page_cursor=&page_number=1&page_size=100",
                "last": "/catalog/v2/products?facet_filters=eyJjYXRlZ29yeSI6WyJIYXJkd2FyZSJdfQ==&page_cursor=Tk9UIElTIFVTRQ==&page_number=82&page_size=100",
                "next": "/catalog/v2/products?facet_filters=eyJjYXRlZ29yeSI6WyJIYXJkd2FyZSJdfQ==&page_cursor=Tk9UIElTIFVTRQ==&page_number=2&page_size=100"
              },
              "data": [
                {
                  "type": "product",
                  "id": 883976,
                  "links": { "self": "/catalog/v2/products/883976" },
                  "relationships": {},
                  "attributes": {
                    "menu": "Hardware/Disco Rígido (HD)/Corporativo",
                    "title": "HD WD Gold Enterprise Class HDD 12TB, 7200 RPM, Cache 512MB, CMR, SATA - WD122KRYZ",
                    "description": "<h2>HD WD Gold 12TB, 7200 RPM, Cache 512MB, CMR, SATA - WD122KRYZ</h2>",
                    "weight": 750,
                    "price": 7777.77,
                    "old_price": 0,
                    "discount_percentage": 10,
                    "price_with_discount": 6999.99,
                    "offer": null,
                    "prime": null,
                    "origin": null,
                    "is_prime": false,
                    "is_openbox": false,
                    "has_free_shipping": false,
                    "has_free_shipping_for_prime_user": false,
                    "is_pre_order": false,
                    "date_pre_order": 0,
                    "available": true,
                    "species": 0,
                    "stock": 2,
                    "limit_buy": 5,
                    "type": 0,
                    "external_url": "",
                    "warranty": "5 anos de garantia (3 meses de garantia legal + 57 meses de garantia contratual junto ao fabricante)",
                    "score_of_ratings": 0,
                    "number_of_ratings": 0,
                    "is_marketplace": false,
                    "marketplace": {},
                    "manufacturer": { "id": 16, "name": "WD", "img": "https://images4.kabum.com.br/produtos/fabricantes/logo-wd.jpg" },
                    "photos": {
                      "p": ["https://images.kabum.com.br/produtos/fotos/883976/hd-wd-gold-enterprise-class-hdd-12tb-7200-rpm-cache-512mb-cmr-sata-wd122kryz_1756492080_p.jpg"],
                      "m": ["https://images.kabum.com.br/produtos/fotos/883976/hd-wd-gold-enterprise-class-hdd-12tb-7200-rpm-cache-512mb-cmr-sata-wd122kryz_1756492080_m.jpg"],
                      "g": ["https://images.kabum.com.br/produtos/fotos/883976/hd-wd-gold-enterprise-class-hdd-12tb-7200-rpm-cache-512mb-cmr-sata-wd122kryz_1756492080_g.jpg"],
                      "gg": ["https://images.kabum.com.br/produtos/fotos/883976/hd-wd-gold-enterprise-class-hdd-12tb-7200-rpm-cache-512mb-cmr-sata-wd122kryz_1756492080_gg.jpg"]
                    },
                    "images": ["https://images.kabum.com.br/produtos/fotos/883976/hd-wd-gold-enterprise-class-hdd-12tb-7200-rpm-cache-512mb-cmr-sata-wd122kryz_1756492080_original.jpg"],
                    "tag_description": "Encontre o HD WD Gold de 12TB para seu data center. Com 7200 RPM e 512MB de cache, oferece máxima confiabilidade e desempenho. Compre já!",
                    "ufs_flash": [],
                    "featured_product": false,
                    "stamps": null,
                    "max_installment": "10x de R$ 777,77",
                    "max_installment_prime": null,
                    "product_link": "hd-wd-gold-enterprise-class-hdd-12tb-7200-rpm-cache-512mb-cmr-sata-wd122kryz",
                    "age_rating": 0,
                    "average_of_ratings": 0
                  }
                },
                {
                  "type": "product",
                  "id": 161693,
                  "links": { "self": "/catalog/v2/products/161693" },
                  "relationships": {},
                  "attributes": {
                    "menu": "Hardware/Placa de vídeo (VGA)/Placa de vídeo AMD",
                    "title": "Placa de Vídeo Afox AMD Radeon R5 230, 1GB DDR3, Low Profile - AFR5230-1024D3L4",
                    "description": "Placa de Video AFOX AMD Radeon R5 230, 1GB, DDR3, Low Profile - AFR5230-1024D3L4",
                    "weight": 1000,
                    "price": 315.5,
                    "old_price": 0,
                    "discount_percentage": 0,
                    "price_with_discount": 315.5,
                    "offer": null,
                    "prime": null,
                    "origin": null,
                    "is_prime": false,
                    "is_openbox": false,
                    "has_free_shipping": false,
                    "has_free_shipping_for_prime_user": false,
                    "is_pre_order": false,
                    "date_pre_order": 0,
                    "available": true,
                    "species": 5,
                    "stock": 1,
                    "limit_buy": 0,
                    "type": 0,
                    "external_url": "",
                    "warranty": "Sem Garantia",
                    "score_of_ratings": 5,
                    "number_of_ratings": 1,
                    "is_marketplace": true,
                    "marketplace": {
                      "seller_id": 2210,
                      "seller_name": "Bits & Bytes",
                      "seller_sale_pj": true,
                      "price": 315.5,
                      "price_origin": 315.5,
                      "product_id": 54162,
                      "code_product_kabum_1P": null,
                      "company": "TRACAO DIGITAL SOLUCOES EM TECNOLOGIA LTDA.",
                      "cnpj": "00.889.039/0001-01",
                      "state": "SP"
                    },
                    "manufacturer": { "id": 2492, "name": "Afox", "img": "https://images4.kabum.com.br/produtos/fabricantes/logo-afox.jpg" },
                    "photos": {
                      "p": ["https://images.kabum.com.br/produtos/fotos/sync_mirakl/161693/Placa-de-V-deo-Afox-AMD-Radeon-R5-230-1GB-DDR3-Low-Profile-AFR5230-1024D3L4_1674570551_p.jpg"],
                      "m": ["https://images.kabum.com.br/produtos/fotos/sync_mirakl/161693/Placa-de-V-deo-Afox-AMD-Radeon-R5-230-1GB-DDR3-Low-Profile-AFR5230-1024D3L4_1674570544_m.jpg"],
                      "g": ["https://images.kabum.com.br/produtos/fotos/sync_mirakl/161693/Placa-de-V-deo-Afox-AMD-Radeon-R5-230-1GB-DDR3-Low-Profile-AFR5230-1024D3L4_1674570538_g.jpg"],
                      "gg": ["https://images.kabum.com.br/produtos/fotos/sync_mirakl/161693/Placa-de-V-deo-Afox-AMD-Radeon-R5-230-1GB-DDR3-Low-Profile-AFR5230-1024D3L4_1674570533_gg.jpg"]
                    },
                    "images": ["https://images.kabum.com.br/produtos/fotos/sync_mirakl/161693/Placa-de-V-deo-Afox-AMD-Radeon-R5-230-1GB-DDR3-Low-Profile-AFR5230-1024D3L4_1674570533_gg.jpg"],
                    "tag_description": "Placa de Video É só no KaBuM!!! Garanta já o seu e pague em até 12x ou da maneira mais rápida, pelo Pix. Tudo para você encontra no KaBuM!",
                    "ufs_flash": [],
                    "featured_product": false,
                    "stamps": null,
                    "max_installment": "10x de R$ 31,55",
                    "max_installment_prime": null,
                    "product_link": "placa-de-video-afox-amd-radeon-r5-230-1gb-ddr3-low-profile-afr5230-1024d3l4",
                    "age_rating": 0,
                    "average_of_ratings": 5
                  }
                },
                {
                  "type": "product",
                  "id": 1059765,
                  "links": { "self": "/catalog/v2/products/1059765" },
                  "relationships": {},
                  "attributes": {
                    "menu": "Hardware/Disco Rígido (HD)/Corporativo/NAS",
                    "title": "HD WD Red Pro, NAS, 26TB, 3.5\\", Cache 512MB, 7200RPM, SATA 6Gb/s - WD260KFGX",
                    "description": "<h2>HD WD Red Pro, NAS, 26TB</h2>",
                    "weight": 660,
                    "price": 11333.32,
                    "old_price": 0,
                    "discount_percentage": 10,
                    "price_with_discount": 10199.99,
                    "offer": null,
                    "prime": {
                      "price": 10766.65,
                      "price_with_discount": 9689.99,
                      "discount_percentage": 5,
                      "save": 510,
                      "is_logged_user_exclusive": true
                    },
                    "origin": null,
                    "is_prime": true,
                    "is_openbox": false,
                    "has_free_shipping": false,
                    "has_free_shipping_for_prime_user": false,
                    "is_pre_order": false,
                    "date_pre_order": 0,
                    "available": true,
                    "species": 0,
                    "stock": 2,
                    "limit_buy": 1,
                    "type": 0,
                    "external_url": "",
                    "warranty": "3 anos de garantia (3 meses de garantia legal + 33 meses de garantia contratual junto ao fabricante)",
                    "score_of_ratings": 0,
                    "number_of_ratings": 0,
                    "is_marketplace": false,
                    "marketplace": {},
                    "manufacturer": { "id": 2869, "name": "Western Digital", "img": "https://images4.kabum.com.br/produtos/fabricantes/logo-nulo.jpg" },
                    "photos": {
                      "p": ["https://images.kabum.com.br/produtos/fotos/1059765/hd-wd-red-pro-nas-26tb-3-5-cache-512mb-7200rpm-sata-6gb-s-wd260kfgx_1787073673_p.jpg"],
                      "m": ["https://images.kabum.com.br/produtos/fotos/1059765/hd-wd-red-pro-nas-26tb-3-5-cache-512mb-7200rpm-sata-6gb-s-wd260kfgx_1787073673_m.jpg"],
                      "g": ["https://images.kabum.com.br/produtos/fotos/1059765/hd-wd-red-pro-nas-26tb-3-5-cache-512mb-7200rpm-sata-6gb-s-wd260kfgx_1787073673_g.jpg"],
                      "gg": ["https://images.kabum.com.br/produtos/fotos/1059765/hd-wd-red-pro-nas-26tb-3-5-cache-512mb-7200rpm-sata-6gb-s-wd260kfgx_1787073673_gg.jpg"]
                    },
                    "images": ["https://images.kabum.com.br/produtos/fotos/1059765/hd-wd-red-pro-nas-26tb-3-5-cache-512mb-7200rpm-sata-6gb-s-wd260kfgx_1787073673_original.jpg"],
                    "tag_description": "HD Western Digital Pro 26TB 7200 RPM, cache 512MB, gravação CMR e otimizado para sistemas NAS RAID ilimitados.",
                    "ufs_flash": [],
                    "featured_product": false,
                    "stamps": null,
                    "max_installment": "10x de R$ 1133,33",
                    "max_installment_prime": "10x de R$ 1076,66",
                    "product_link": "hd-wd-red-pro-nas-26tb-3-5-cache-512mb-7200rpm-sata-6gb-s-wd260kfgx",
                    "age_rating": 0,
                    "average_of_ratings": 0
                  }
                }
              ]
            }
            """;

    @Test
    void buscarPaginaHardware_deveDesserializarRespostaReal() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/catalog/v2/products"))
                .withQueryParam("facet_filters", equalTo(FACET_FILTERS_HARDWARE))
                .withQueryParam("page_number", equalTo("1"))
                .withQueryParam("page_size", equalTo("100"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_CATALOGO_HARDWARE)));

        KabumCatalogoResponse resposta = kabumClient.buscarPaginaHardware(1, 100);

        assertThat(resposta.meta().totalItemsCount()).isEqualTo(8155);
        assertThat(resposta.meta().totalPagesCount()).isEqualTo(82);
        assertThat(resposta.meta().page().number()).isEqualTo(1);
        assertThat(resposta.data()).hasSize(3);

        KabumProdutoResponse descontoSimples = resposta.data().get(0);
        assertThat(descontoSimples.id()).isEqualTo(883976);
        assertThat(descontoSimples.attributes().menu()).isEqualTo("Hardware/Disco Rígido (HD)/Corporativo");
        assertThat(descontoSimples.attributes().price()).isEqualByComparingTo(new BigDecimal("7777.77"));
        assertThat(descontoSimples.attributes().priceWithDiscount()).isEqualByComparingTo(new BigDecimal("6999.99"));
        assertThat(descontoSimples.attributes().available()).isTrue();
        assertThat(descontoSimples.attributes().stock()).isEqualTo(2);
        assertThat(descontoSimples.attributes().isMarketplace()).isFalse();
        assertThat(descontoSimples.attributes().productLink())
                .isEqualTo("hd-wd-gold-enterprise-class-hdd-12tb-7200-rpm-cache-512mb-cmr-sata-wd122kryz");
        assertThat(descontoSimples.attributes().prime()).isNull();

        KabumProdutoResponse marketplace = resposta.data().get(1);
        assertThat(marketplace.id()).isEqualTo(161693);
        assertThat(marketplace.attributes().menu()).isEqualTo("Hardware/Placa de vídeo (VGA)/Placa de vídeo AMD");
        assertThat(marketplace.attributes().price()).isEqualByComparingTo(new BigDecimal("315.5"));
        assertThat(marketplace.attributes().priceWithDiscount()).isEqualByComparingTo(new BigDecimal("315.5"));
        assertThat(marketplace.attributes().stock()).isEqualTo(1);
        assertThat(marketplace.attributes().isMarketplace()).isTrue();

        KabumProdutoResponse comPrime = resposta.data().get(2);
        assertThat(comPrime.id()).isEqualTo(1059765);
        assertThat(comPrime.attributes().price()).isEqualByComparingTo(new BigDecimal("11333.32"));
        assertThat(comPrime.attributes().priceWithDiscount()).isEqualByComparingTo(new BigDecimal("10199.99"));
        assertThat(comPrime.attributes().prime()).isNotNull();
        assertThat(comPrime.attributes().prime().priceWithDiscount()).isEqualByComparingTo(new BigDecimal("9689.99"));
        assertThat(comPrime.attributes().prime().isLoggedUserExclusive()).isTrue();
    }

    @Test
    void buscarPaginaHardware_deveEnviarFacetFiltersDoCategoriaHardwareCodificadoEmBase64() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/catalog/v2/products"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_CATALOGO_HARDWARE)));

        kabumClient.buscarPaginaHardware(1, 100);

        WIREMOCK.verify(getRequestedFor(urlPathEqualTo("/catalog/v2/products"))
                .withQueryParam("facet_filters", equalTo(FACET_FILTERS_HARDWARE)));
    }

    @Test
    void erroHttp_500_deveLancarKabumApiExceptionComStatus500() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/catalog/v2/products"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> kabumClient.buscarPaginaHardware(1, 100))
                .isInstanceOf(KabumApiException.class)
                .satisfies(ex -> assertThat(((KabumApiException) ex).statusCode()).isEqualTo(500));
    }

    @Test
    void erroHttp_429_deveLancarKabumApiExceptionComStatus429() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/catalog/v2/products"))
                .willReturn(aResponse().withStatus(429)));

        assertThatThrownBy(() -> kabumClient.buscarPaginaHardware(1, 100))
                .isInstanceOf(KabumApiException.class)
                .satisfies(ex -> assertThat(((KabumApiException) ex).statusCode()).isEqualTo(429));
    }

    @Test
    void corpoInvalidoComStatus200_deveLancarKabumApiExceptionComStatusSentinela() {
        WIREMOCK.stubFor(get(urlPathEqualTo("/catalog/v2/products"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("isso nao eh json")));

        assertThatThrownBy(() -> kabumClient.buscarPaginaHardware(1, 100))
                .isInstanceOf(KabumApiException.class)
                .satisfies(ex -> assertThat(((KabumApiException) ex).statusCode()).isEqualTo(0));
    }
}
