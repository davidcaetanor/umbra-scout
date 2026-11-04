package dev.davidcaetano.umbra_api.catalogo.controller;

import dev.davidcaetano.umbra_api.comum.WireMockIntegrationTestBase;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class BuscaExternaControllerTest extends WireMockIntegrationTestBase {

    private static final String FIXTURE_BUSCA = fixture("wiremock/itad/games-search-v1-elden-ring.json");
    private static final String FIXTURE_PRECOS = fixture("wiremock/itad/games-prices-v3-elden-ring.json");

    private static final String GID_ELDEN_RING = "018d937f-590c-728b-ac35-38bcff85f086";
    private static final String GID_NIGHTREIGN = "0193bdcf-1842-73b6-aa11-bf0ebd540557";
    private static final String GID_NIGHTREIGN_DELUXE = "0194fa78-67be-7370-81b0-53debfa591b9";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    @BeforeEach
    void montarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void incluirExterno_deveTrazerOsJogosDoItadSemGravarNada() throws Exception {
        stubItad("t212c ctrl externos");
        List<Long> antes = contagens();

        mockMvc.perform(get("/produtos").param("nome", "t212c ctrl externos").param("incluirExterno", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isEmpty())
                .andExpect(jsonPath("$.externos.length()").value(3))
                .andExpect(jsonPath("$.externos[0].gid").value(GID_ELDEN_RING))
                .andExpect(jsonPath("$.externos[0].nome").value("Elden Ring"))
                .andExpect(jsonPath("$.externos[0].melhorPreco.loja").value("Nuuvem"))
                .andExpect(jsonPath("$.externos[0].melhorPreco.valorCentavos").value(24705))
                .andExpect(jsonPath("$.externos[0].melhorPreco.valorOriginalCentavos").value(27450))
                .andExpect(jsonPath("$.externos[0].melhorPreco.gratuito").value(false))
                .andExpect(jsonPath("$.externos[0].melhorPreco.coletadoEm").isNotEmpty())
                .andExpect(jsonPath("$.externos[2].gid").value(GID_NIGHTREIGN_DELUXE))
                .andExpect(jsonPath("$.externos[2].melhorPreco.loja").value("Steam"));

        assertThat(contagens()).isEqualTo(antes);
    }

    @Test
    void jogoExternoJaNoCatalogo_naoDeveAparecerEmExternos() throws Exception {
        Integer existentes = jdbc.queryForObject("SELECT count(*) FROM produto WHERE chave_itad = ?", Integer.class,
                GID_ELDEN_RING);
        if (existentes == 0) {
            jdbc.update("INSERT INTO produto (tipo, nome, chave_itad) VALUES ('JOGO', 't212c-ctrl Elden Ring', ?)",
                    GID_ELDEN_RING);
        }
        stubItad("t212c ctrl no catalogo");

        mockMvc.perform(get("/produtos").param("nome", "t212c ctrl no catalogo").param("incluirExterno", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externos[*].gid").value(not(hasItem(GID_ELDEN_RING))))
                .andExpect(jsonPath("$.externos[0].gid").value(GID_NIGHTREIGN))
                .andExpect(jsonPath("$.externos.length()").value(2));
    }

    @Test
    void semIncluirExterno_naoDeveConsultarOItad() throws Exception {
        stubItad("t212c ctrl sem parametro");

        mockMvc.perform(get("/produtos").param("nome", "t212c ctrl sem parametro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externos").isEmpty());

        WIREMOCK.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void paginaDiferenteDaPrimeira_naoDeveConsultarOItad() throws Exception {
        stubItad("t212c ctrl pagina um");

        mockMvc.perform(get("/produtos").param("nome", "t212c ctrl pagina um").param("incluirExterno", "true")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externos").isEmpty());

        WIREMOCK.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void itadEm500_deveResponder200ComExternosVazio() throws Exception {
        WIREMOCK.stubFor(WireMock.get(urlPathEqualTo("/games/search/v1")).willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get("/produtos").param("nome", "t212c ctrl fora do ar").param("incluirExterno", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externos").isEmpty());

        WIREMOCK.verify(1, getRequestedFor(urlPathEqualTo("/games/search/v1")));
    }

    private List<Long> contagens() {
        return List.of(
                jdbc.queryForObject("SELECT count(*) FROM produto", Long.class),
                jdbc.queryForObject("SELECT count(*) FROM oferta", Long.class),
                jdbc.queryForObject("SELECT count(*) FROM preco", Long.class));
    }

    private static void stubItad(String termo) {
        WIREMOCK.stubFor(WireMock.get(urlPathEqualTo("/games/search/v1"))
                .withQueryParam("title", equalTo(termo))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_BUSCA)));
        WIREMOCK.stubFor(post(urlPathEqualTo("/games/prices/v3"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_PRECOS)));
    }
}
