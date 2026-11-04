package dev.davidcaetano.umbra_api.catalogo.controller;

import com.jayway.jsonpath.JsonPath;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HistoricoControllerTest extends IntegrationTestBase {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    private final Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @BeforeEach
    void montarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void ofertaSemMudancaNaJanela_deveEntrarComOValorAnteriorNoInicioDoPeriodo() throws Exception {
        long id = produto("t212b-c19 Sem mudanca", true);
        preco(oferta(id, "STEAM", true), 10000, true, diasAtras(60));

        String json = historico(id, "30d");

        Instant inicioDaJanela = Instant.parse(JsonPath.read(json, "$.consultadoEm"))
                .atZone(ZoneOffset.UTC).minusDays(30).toInstant();
        assertThat(pontos(json, "$.melhorPreco")).containsExactly(Map.entry(inicioDaJanela, 10000L));
        assertThat(pontos(json, "$.porLoja[0].pontos")).containsExactly(Map.entry(inicioDaJanela, 10000L));
        assertThat(Instant.parse(JsonPath.read(json, "$.observadoDesde"))).isEqualTo(diasAtras(60));
    }

    @Test
    void linhaIndisponivel_deveVirarLacunaENaoZero() throws Exception {
        long id = produto("t212b-c20 Lacuna", true);
        long oferta = oferta(id, "KABUM", true);
        preco(oferta, 30000, true, diasAtras(20));
        preco(oferta, 0, false, diasAtras(10));
        preco(oferta, 32000, true, diasAtras(5));

        String json = historico(id, "30d");

        assertThat(JsonPath.<List<Object>>read(json, "$.melhorPreco[*].valorCentavos"))
                .containsExactly(30000, null, 32000);
        assertThat(JsonPath.<List<Object>>read(json, "$.porLoja[0].pontos[*].valorCentavos"))
                .containsExactly(30000, null, 32000);
        assertThat(JsonPath.<List<Object>>read(json, "$.faixas")).containsExactly(30000, 30500, 31000, 31500, 32000);
    }

    @Test
    void duasLinhasNoMesmoInstante_deveFicarADeMaiorId() throws Exception {
        long id = produto("t212b-c21 Mesmo instante", true);
        long oferta = oferta(id, "STEAM", true);
        preco(oferta, 9000, true, diasAtras(3));
        preco(oferta, 8500, true, diasAtras(3));

        String json = historico(id, "30d");

        assertThat(pontos(json, "$.melhorPreco")).containsExactly(Map.entry(diasAtras(3), 8500L));
    }

    @Test
    void minimo_deveIgnorarOfertaInativaELinhaIndisponivelETrazerLojaEPrimeiraData() throws Exception {
        long id = produto("t212b-c22 Minimo", true);
        long steam = oferta(id, "STEAM", true);
        preco(steam, 3000, true, diasAtras(50));
        preco(steam, 2000, true, diasAtras(40));
        preco(steam, 2500, true, diasAtras(30));
        preco(steam, 2000, true, diasAtras(20));
        preco(steam, 100, false, diasAtras(10));
        preco(oferta(id, "NUUVEM", false), 500, true, diasAtras(15));

        mockMvc.perform(get("/produtos/{id}/historico", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimo.valorCentavos").value(2000))
                .andExpect(jsonPath("$.minimo.loja").value("Steam"))
                .andExpect(jsonPath("$.minimo.gratuito").value(false))
                .andExpect(jsonPath("$.minimo.em").value(diasAtras(40).toString()))
                .andExpect(jsonPath("$.porLoja.length()").value(1));
    }

    @Test
    void variacao_deveCompararOPrecoAtualComOMinimo() throws Exception {
        long id = produto("t212b-c23 Variacao", true);
        long oferta = oferta(id, "STEAM", true);
        preco(oferta, 10000, true, diasAtras(10));
        preco(oferta, 11000, true, diasAtras(1));

        mockMvc.perform(get("/produtos/{id}/historico", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variacao.precoAtualCentavos").value(11000))
                .andExpect(jsonPath("$.variacao.diferencaCentavos").value(1000))
                .andExpect(jsonPath("$.variacao.percentualAcimaDoMinimo").value(10.0));
    }

    @Test
    void produtoEsgotado_deveTerVariacaoNula() throws Exception {
        long id = produto("t212b-c23 Esgotado", true);
        long oferta = oferta(id, "KABUM", true);
        preco(oferta, 50000, true, diasAtras(10));
        preco(oferta, 0, false, diasAtras(1));

        mockMvc.perform(get("/produtos/{id}/historico", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimo.valorCentavos").value(50000))
                .andExpect(jsonPath("$.variacao.precoAtualCentavos").value(nullValue()))
                .andExpect(jsonPath("$.variacao.diferencaCentavos").value(nullValue()))
                .andExpect(jsonPath("$.variacao.percentualAcimaDoMinimo").value(nullValue()));
    }

    @Test
    void periodoTudo_naoDeveUsarPontoDeEntrada() throws Exception {
        long id = produto("t212b-c24 Tudo", true);
        long oferta = oferta(id, "STEAM", true);
        preco(oferta, 5000, true, diasAtras(400));
        preco(oferta, 6000, true, diasAtras(5));

        String json = historico(id, "tudo");

        assertThat(JsonPath.<String>read(json, "$.periodo")).isEqualTo("tudo");
        assertThat(pontos(json, "$.melhorPreco"))
                .containsExactly(Map.entry(diasAtras(400), 5000L), Map.entry(diasAtras(5), 6000L));
    }

    @Test
    void periodoPadrao_deveSer90d() throws Exception {
        long id = produto("t212b-c24 Padrao", true);
        preco(oferta(id, "STEAM", true), 5000, true, diasAtras(1));

        mockMvc.perform(get("/produtos/{id}/historico", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("90d"));
    }

    @Test
    void periodoForaDaLista_deveDevolver400ListandoOsAceitos() throws Exception {
        long id = produto("t212b-c24 Periodo invalido", true);

        mockMvc.perform(get("/produtos/{id}/historico", id).param("periodo", "7d"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://umbra.dev/erros/parametro-invalido"))
                .andExpect(jsonPath("$.erros[0].campo").value("periodo"))
                .andExpect(jsonPath("$.erros[0].mensagem").value(containsString("30d, 90d, 1a, tudo")));
    }

    @Test
    void produtoInexistenteOuInativo_deveDevolver404() throws Exception {
        long inativo = produto("t212b-c25 Inativo", false);
        long inexistente = jdbc.queryForObject("SELECT coalesce(max(id), 0) + 1000 FROM produto", Long.class);

        for (long id : List.of(inativo, inexistente)) {
            mockMvc.perform(get("/produtos/{id}/historico", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("https://umbra.dev/erros/recurso-nao-encontrado"))
                    .andExpect(jsonPath("$.instance").value("/produtos/%d/historico".formatted(id)));
        }
    }

    @Test
    void produtoComUmPontoSo_deveTrazerASerieDeUmPontoEObservadoDesde() throws Exception {
        long id = produto("t212b-c25 Um ponto", true);
        preco(oferta(id, "GOG", true), 7990, true, diasAtras(2));

        String json = historico(id, "30d");

        assertThat(pontos(json, "$.melhorPreco")).containsExactly(Map.entry(diasAtras(2), 7990L));
        assertThat(Instant.parse(JsonPath.read(json, "$.observadoDesde"))).isEqualTo(diasAtras(2));
        assertThat(JsonPath.<List<Object>>read(json, "$.faixas")).containsExactly(7990, 7990, 7990, 7990, 7990);
    }

    @Test
    void produtoSemPreco_deveDevolver200ComSeriesVazias() throws Exception {
        long id = produto("t212b-c25 Sem preco", true);

        mockMvc.perform(get("/produtos/{id}/historico", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.melhorPreco").isEmpty())
                .andExpect(jsonPath("$.porLoja").isEmpty())
                .andExpect(jsonPath("$.faixas").isEmpty())
                .andExpect(jsonPath("$.minimo").value(nullValue()))
                .andExpect(jsonPath("$.observadoDesde").value(nullValue()));
    }

    private String historico(long id, String periodo) throws Exception {
        return mockMvc.perform(get("/produtos/{id}/historico", id).param("periodo", periodo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static List<Map.Entry<Instant, Long>> pontos(String json, String caminho) {
        List<Map<String, Object>> pontos = JsonPath.read(json, caminho);
        return pontos.stream()
                .map(ponto -> Map.entry(Instant.parse((String) ponto.get("instante")),
                        ((Number) ponto.get("valorCentavos")).longValue()))
                .toList();
    }

    private Instant diasAtras(int dias) {
        return agora.minus(Duration.ofDays(dias));
    }

    private long produto(String nome, boolean ativo) {
        return jdbc.queryForObject("""
                INSERT INTO produto (tipo, nome, ativo) VALUES ('JOGO', ?, ?) RETURNING id
                """, Long.class, nome, ativo);
    }

    private long oferta(long produtoId, String codigoLoja, boolean ativa) {
        String identificador = UUID.randomUUID().toString();
        return jdbc.queryForObject("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url, ativa)
                VALUES (?, (SELECT id FROM loja WHERE codigo = ?), ?, ?, ?)
                RETURNING id
                """, Long.class, produtoId, codigoLoja, identificador, "https://loja.test/" + identificador, ativa);
    }

    private void preco(long ofertaId, long valorCentavos, boolean disponivel, Instant coletadoEm) {
        jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, disponivel, origem_coleta, coletado_em)
                VALUES (?, ?, ?, 'ITAD_API', ?)
                """, ofertaId, valorCentavos, disponivel, Timestamp.from(coletadoEm));
    }
}
