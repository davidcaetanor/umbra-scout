package dev.davidcaetano.umbra_api.monitoramento.controller;

import dev.davidcaetano.umbra_api.comum.WireMockIntegrationTestBase;
import dev.davidcaetano.umbra_api.usuario.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MonitoramentoJogoExternoTest extends WireMockIntegrationTestBase {

    private static final String GID = "018d937f-590c-728b-ac35-38bcff85f086";
    private static final String INFO = fixture("wiremock/itad/games-info-v2-elden-ring.json");
    private static final String PRECOS = fixture("wiremock/itad/games-prices-v3-elden-ring.json");
    private static final String LOOKUP = "{\"" + GID + "\": [\"app/1245620\"]}";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        limpar();
    }

    @AfterEach
    void limpar() {
        jdbc.update("DELETE FROM usuario WHERE email LIKE '%@externo.umbra.dev'");
        jdbc.update("""
                DELETE FROM preco WHERE oferta_id IN (
                    SELECT o.id FROM oferta o JOIN produto p ON p.id = o.produto_id WHERE p.chave_itad = ?)
                """, GID);
        jdbc.update("DELETE FROM oferta WHERE produto_id IN (SELECT id FROM produto WHERE chave_itad = ?)", GID);
        jdbc.update("DELETE FROM produto WHERE chave_itad = ?", GID);
    }

    private void itadResponde(String info, String precos) {
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/info/v2")).willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(info)));
        WIREMOCK.stubFor(post(urlPathEqualTo("/games/prices/v3")).willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(precos)));
        WIREMOCK.stubFor(post(urlPathMatching("/lookup/shop/\\d+/id/v1")).willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(LOOKUP)));
    }

    private Long inserirUsuario(String nome) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (email, senha_hash, nome_exibicao)
                VALUES (?, 'hash', 'Pessoa')
                RETURNING id
                """, Long.class, nome + "@externo.umbra.dev");
    }

    private ResultActions monitorarGid(Long usuario, String gid) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/monitoramentos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.emitir(usuario).valor())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gid\": \"%s\", \"precoAlvoCentavos\": 15000}".formatted(gid)));
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class, GID);
    }

    private int produtos() {
        return contar("SELECT count(*) FROM produto WHERE chave_itad = ?");
    }

    @Test
    void jogoForaDoCatalogo_deveCriarProdutoOfertasPrimeiroPrecoEMonitoramentoNaMesmaChamada() throws Exception {
        itadResponde(INFO, PRECOS);
        Long usuario = inserirUsuario("novo");

        monitorarGid(usuario, GID)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("ELDEN RING"))
                .andExpect(jsonPath("$.precoAlvoCentavos").value(15000))
                .andExpect(jsonPath("$.melhorPreco.valorCentavos").value(24705));

        assertThat(produtos()).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM oferta o JOIN produto p ON p.id = o.produto_id WHERE p.chave_itad = ?"))
                .isEqualTo(2);
        assertThat(contar("""
                SELECT count(*) FROM preco pr JOIN oferta o ON o.id = pr.oferta_id
                  JOIN produto p ON p.id = o.produto_id WHERE p.chave_itad = ? AND pr.origem_coleta = 'ITAD_API'
                """)).isEqualTo(2);
        assertThat(contar("""
                SELECT count(*) FROM monitoramento m JOIN produto p ON p.id = m.produto_id WHERE p.chave_itad = ?
                """)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT imagem_url FROM produto WHERE chave_itad = ?", String.class, GID))
                .endsWith("/boxart.jpg");
    }

    @Test
    void jogoForaDoCatalogo_deveEntrarNoConjuntoQueORefreshDoItadAtualiza() throws Exception {
        itadResponde(INFO, PRECOS);

        monitorarGid(inserirUsuario("refresh"), GID).andExpect(status().isCreated());

        assertThat(contar("""
                SELECT count(*) FROM oferta o JOIN produto p ON p.id = o.produto_id
                 WHERE p.chave_itad = ? AND p.ativo AND o.ativa
                """)).isEqualTo(2);
    }

    @Test
    void jogoJaNoCatalogo_naoDeveChamarOItad() throws Exception {
        itadResponde(INFO, PRECOS);
        monitorarGid(inserirUsuario("primeiro"), GID).andExpect(status().isCreated());
        WIREMOCK.resetRequests();

        monitorarGid(inserirUsuario("segundo"), GID).andExpect(status().isCreated());

        WIREMOCK.verify(0, getRequestedFor(urlPathEqualTo("/games/info/v2")));
        WIREMOCK.verify(0, postRequestedFor(urlPathEqualTo("/games/prices/v3")));
        assertThat(produtos()).isEqualTo(1);
    }

    @Test
    void gidDesconhecidoNoItad_deveResponder404SemGravar() throws Exception {
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/info/v2")).willReturn(aResponse().withStatus(404)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("{\"status_code\":404,\"reason_phrase\":\"Not Found\"}")));

        monitorarGid(inserirUsuario("desconhecido"), GID)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://umbra.dev/erros/recurso-nao-encontrado"));

        assertThat(produtos()).isZero();
        WIREMOCK.verify(0, postRequestedFor(urlPathEqualTo("/games/prices/v3")));
    }

    @Test
    void itemQueNaoEJogo_deveResponder422SemGravar() throws Exception {
        itadResponde(INFO.replace("\"type\":\"game\"", "\"type\":\"dlc\""), PRECOS);

        monitorarGid(inserirUsuario("dlc"), GID)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value("https://umbra.dev/erros/regra-de-negocio-violada"));

        assertThat(produtos()).isZero();
    }

    @Test
    void jogoSemOfertaNasLojasAcompanhadas_deveResponder422SemGravar() throws Exception {
        itadResponde(INFO, "[{\"id\":\"" + GID + "\",\"deals\":[]}]");

        monitorarGid(inserirUsuario("semoferta"), GID).andExpect(status().isUnprocessableContent());

        assertThat(produtos()).isZero();
    }

    @Test
    void itadFora_deveResponder503ComRetryAfterSemGravar() throws Exception {
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/info/v2")).willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(INFO)));
        WIREMOCK.stubFor(post(urlPathEqualTo("/games/prices/v3")).willReturn(aResponse().withStatus(500)));

        monitorarGid(inserirUsuario("fora"), GID)
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
                .andExpect(jsonPath("$.type").value("https://umbra.dev/erros/servico-indisponivel"));

        assertThat(produtos()).isZero();
        WIREMOCK.verify(1, postRequestedFor(urlPathEqualTo("/games/prices/v3")));
    }

    @Test
    void produtoIdEGidJuntos_deveResponder400() throws Exception {
        Long usuario = inserirUsuario("ambos");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/monitoramentos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.emitir(usuario).valor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\": 1, \"gid\": \"%s\", \"precoAlvoCentavos\": 1000}".formatted(GID)))
                .andExpect(status().isBadRequest());
        monitorarGid(usuario, "nao-e-uuid").andExpect(status().isBadRequest());
    }

    @Test
    void contaDesativada_naoDeveChamarOItadNemGravarProduto() throws Exception {
        itadResponde(INFO, PRECOS);
        Long usuario = inserirUsuario("inativa");
        jdbc.update("UPDATE usuario SET ativo = FALSE WHERE id = ?", usuario);

        monitorarGid(usuario, GID).andExpect(status().isNotFound());

        WIREMOCK.verify(0, getRequestedFor(urlPathEqualTo("/games/info/v2")));
        assertThat(produtos()).isZero();
    }

    @Test
    void doisUsuariosMonitorandoOMesmoJogoExternoAoMesmoTempo_terminamComUmProdutoSo() throws Exception {
        itadResponde(INFO, PRECOS);
        Long primeiro = inserirUsuario("corrida-a");
        Long segundo = inserirUsuario("corrida-b");
        CyclicBarrier largada = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<MvcResult>> respostas = executor.invokeAll(List.of(
                    corrida(largada, primeiro), corrida(largada, segundo)), 30, TimeUnit.SECONDS);
            for (Future<MvcResult> resposta : respostas) {
                assertThat(resposta.get().getResponse().getStatus()).isEqualTo(201);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(produtos()).isEqualTo(1);
        assertThat(contar("""
                SELECT count(*) FROM monitoramento m JOIN produto p ON p.id = m.produto_id WHERE p.chave_itad = ?
                """)).isEqualTo(2);
    }

    private Callable<MvcResult> corrida(CyclicBarrier largada, Long usuario) {
        return () -> {
            largada.await(10, TimeUnit.SECONDS);
            return monitorarGid(usuario, GID).andReturn();
        };
    }

    @Test
    void gidAleatorioValido_naoDeveConfundirComOutroProduto() throws Exception {
        String outroGid = UUID.randomUUID().toString();
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/info/v2")).willReturn(aResponse().withStatus(404)));

        monitorarGid(inserirUsuario("aleatorio"), outroGid).andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM produto WHERE chave_itad = ?", Integer.class, outroGid))
                .isZero();
    }
}
