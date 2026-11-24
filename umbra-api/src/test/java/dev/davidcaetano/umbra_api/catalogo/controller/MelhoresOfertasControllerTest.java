package dev.davidcaetano.umbra_api.catalogo.controller;

import com.jayway.jsonpath.JsonPath;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MelhoresOfertasControllerTest extends IntegrationTestBase {

    private static final Instant AGORA = Instant.now();

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
    void economiaEmReais_deveVencerOPercentual_comAMesmaProximidadeDoMinimo() throws Exception {
        long caroComPoucoDesconto = jogoComPreco("t44b-a1 Caro", "STEAM", 24000, 30000, 20);
        long baratoComMuitoDesconto = jogoComPreco("t44b-a1 Barato", "STEAM", 500, 5000, 90);

        assertThat(ids(buscar("t44b-a1", "melhoresOfertas")))
                .containsExactly(caroComPoucoDesconto, baratoComMuitoDesconto);
    }

    @Test
    void comAMesmaEconomia_oQueEstaNoMinimoHistoricoVemAntes() throws Exception {
        long acimaDoMinimo = jogoComPreco("t44b-a2 Acima", "STEAM", 6000, 9000, 33);
        preco(ofertaDe(acimaDoMinimo), 3000, 9000, 67, AGORA.minus(Duration.ofDays(5)));
        long noMinimo = jogoComPreco("t44b-a2 No minimo", "STEAM", 6000, 9000, 33);

        assertThat(ids(buscar("t44b-a2", "melhoresOfertas"))).containsExactly(noMinimo, acimaDoMinimo);
    }

    @Test
    void melhoresOfertas_deveDeixarDeForaQuemNaoTemDesconto() throws Exception {
        long comDesconto = jogoComPreco("t44b-a3 Com desconto", "STEAM", 4000, 8000, 50);
        jogoComPreco("t44b-a3 Preco cheio", "STEAM", 4000, 4000, 0);
        produto("t44b-a3 Sem preco", "JOGO");

        assertThat(ids(buscar("t44b-a3", "melhoresOfertas"))).containsExactly(comDesconto);
    }

    @Test
    void promocaoJaEncerrada_naoEntraNasMelhoresOfertas() throws Exception {
        long vigente = jogoComPreco("t44b-a5 Vigente", "STEAM", 4000, 8000, 50);
        long encerrada = produto("t44b-a5 Encerrada", "JOGO");
        jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, valor_original_centavos, desconto_pct, disponivel,
                                   origem_coleta, expira_em, coletado_em)
                VALUES (?, 1000, 9000, 89, true, 'ITAD_API', ?, ?)
                """, oferta(encerrada, "STEAM"), Timestamp.from(AGORA.minus(Duration.ofHours(2))),
                Timestamp.from(AGORA.minus(Duration.ofDays(1))));

        assertThat(ids(buscar("t44b-a5", "melhoresOfertas"))).containsExactly(vigente);
    }

    @Test
    void jogoGratuitoEmPromocao_entraSemErroDeDivisao() throws Exception {
        long gratis = jogoComPreco("t44b-a4 Gratis", "EPIC", 0, 7000, 100);

        assertThat(ids(buscar("t44b-a4", "melhoresOfertas"))).containsExactly(gratis);
    }

    @Test
    void maiorDesconto_deveOrdenarPeloPercentual() throws Exception {
        long vinte = jogoComPreco("t44b-b1 Vinte", "STEAM", 24000, 30000, 20);
        long noventa = jogoComPreco("t44b-b1 Noventa", "STEAM", 500, 5000, 90);
        long semDesconto = jogoComPreco("t44b-b1 Zero", "STEAM", 1000, 1000, 0);

        assertThat(ids(buscar("t44b-b1", "maiorDesconto"))).containsExactly(noventa, vinte, semDesconto);
    }

    @ParameterizedTest
    @CsvSource({"ordenacao,melhoresOfertas", "ordenacao,maiorDesconto", "descontoMin,10"})
    void criterioDeOferta_semTipoJogo_deveDevolver400(String parametro, String valor) throws Exception {
        mockMvc.perform(get("/produtos").param(parametro, valor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("criterioDeOfertaComTipoJogo")));
        mockMvc.perform(get("/produtos").param("tipo", "HARDWARE").param(parametro, valor))
                .andExpect(status().isBadRequest());
    }

    @Test
    void faixaDePreco_deveIncluirAsPontasEExcluirOResto() throws Exception {
        jogoComPreco("t44b-c1 Abaixo", "STEAM", 999, 2000, 50);
        long naPontaDeBaixo = jogoComPreco("t44b-c1 Ponta baixa", "STEAM", 1000, 2000, 50);
        long naPontaDeCima = jogoComPreco("t44b-c1 Ponta alta", "STEAM", 5000, 9000, 44);
        jogoComPreco("t44b-c1 Acima", "STEAM", 5001, 9000, 44);
        produto("t44b-c1 Sem preco", "JOGO");

        String json = conteudoDe(get("/produtos").param("nome", "t44b-c1").param("ordenacao", "menorPreco")
                .param("precoMin", "1000").param("precoMax", "5000"));

        assertThat(ids(json)).containsExactly(naPontaDeBaixo, naPontaDeCima);
    }

    @Test
    void faixaDePrecoInvertidaOuNegativa_deveDevolver400() throws Exception {
        mockMvc.perform(get("/produtos").param("precoMin", "5000").param("precoMax", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("faixaDePrecoValida")));
        mockMvc.perform(get("/produtos").param("precoMin", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("precoMin")));
    }

    @Test
    void descontoMinimo_deveFiltrarPeloPercentual() throws Exception {
        jogoComPreco("t44b-c2 Pouco", "STEAM", 9000, 10000, 10);
        long bastante = jogoComPreco("t44b-c2 Bastante", "STEAM", 5000, 10000, 50);

        String json = conteudoDe(get("/produtos").param("nome", "t44b-c2").param("tipo", "JOGO")
                .param("descontoMin", "50"));

        assertThat(ids(json)).containsExactly(bastante);
    }

    @Test
    void descontoMinimoForaDaFaixa_deveDevolver400() throws Exception {
        mockMvc.perform(get("/produtos").param("tipo", "JOGO").param("descontoMin", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/produtos").param("tipo", "JOGO").param("descontoMin", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loja_deveUsarOPrecoDessaLojaMesmoComOutraMaisBarata() throws Exception {
        long jogo = jogoComPreco("t44b-d1 Em duas lojas", "NUUVEM", 3000, 6000, 50);
        preco(oferta(jogo, "STEAM"), 4500, 6000, 25, AGORA.minus(Duration.ofHours(1)));
        jogoComPreco("t44b-d1 So na Nuuvem", "NUUVEM", 1000, 2000, 50);

        String json = conteudoDe(get("/produtos").param("nome", "t44b-d1").param("loja", "STEAM"));

        assertThat(ids(json)).containsExactly(jogo);
        assertThat(JsonPath.<String>read(json, "$.conteudo[0].melhorPreco.loja")).isEqualTo("Steam");
        assertThat(JsonPath.<Integer>read(json, "$.conteudo[0].melhorPreco.valorCentavos")).isEqualTo(4500);
        assertThat(JsonPath.<Integer>read(json, "$.totalElementos")).isEqualTo(1);
    }

    @Test
    void lojaInvalida_deveDevolver400() throws Exception {
        mockMvc.perform(get("/produtos").param("loja", "LOJA_QUE_NAO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void melhoresOfertas_deveSerEstavelEntreAsPaginasComIdDeDesempate() throws Exception {
        List<Long> esperados = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            esperados.add(jogoComPreco("t44b-e1 Empate %02d".formatted(i), "STEAM", 2000, 4000, 50));
        }

        List<Long> vistos = new ArrayList<>();
        for (int pagina = 0; pagina < 2; pagina++) {
            vistos.addAll(ids(conteudoDe(get("/produtos").param("nome", "t44b-e1").param("tipo", "JOGO")
                    .param("ordenacao", "melhoresOfertas").param("page", String.valueOf(pagina)))));
        }

        assertThat(vistos).containsExactlyElementsOf(esperados);
    }

    @Test
    void destaques_devemTrazerUmaEdicaoPorJogoEAMelhorDelas() throws Exception {
        long ultimate = jogoComPreco("T44B Zeta Saga: Ultimate Edition", "STEAM", 10000, 9_000_000, 99);
        jogoComPreco("T44B Zeta Saga Deluxe Edition", "STEAM", 10000, 8_000_000, 99);
        jogoComPreco("T44B Zeta Saga", "STEAM", 10000, 7_000_000, 99);
        long outroJogo = jogoComPreco("T44B Outro Jogo", "EPIC", 10000, 6_000_000, 99);

        String json = conteudoDe(get("/produtos/destaques"));
        List<Long> destaques = ids(json);

        assertThat(destaques).hasSizeLessThanOrEqualTo(8).startsWith(ultimate, outroJogo);
        assertThat(JsonPath.<List<String>>read(json, "$.conteudo[*].nome"))
                .filteredOn(nome -> nome.startsWith("T44B Zeta"))
                .containsExactly("T44B Zeta Saga: Ultimate Edition");
    }

    @Test
    void ordemPorNome_naoDiferenciaMaiusculaNemAcento() throws Exception {
        long witch = jogoComPreco("t44b-f1 Witch Cats", "STEAM", 1000, 1000, 0);
        long hidden = jogoComPreco("t44b-f1 hidden frogs", "STEAM", 1000, 1000, 0);
        long ilha = jogoComPreco("t44b-f1 Ílha", "STEAM", 1000, 1000, 0);
        long abismo = jogoComPreco("t44b-f1 abismo", "STEAM", 1000, 1000, 0);

        assertThat(ids(buscar("t44b-f1", "nome"))).containsExactly(abismo, hidden, ilha, witch);
    }

    private String buscar(String termo, String ordenacao) throws Exception {
        return conteudoDe(get("/produtos").param("nome", termo).param("tipo", "JOGO").param("ordenacao", ordenacao));
    }

    private String conteudoDe(MockHttpServletRequestBuilder requisicao) throws Exception {
        return mockMvc.perform(requisicao).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static List<Long> ids(String json) {
        List<Number> ids = JsonPath.read(json, "$.conteudo[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    private long jogoComPreco(String nome, String loja, long valor, long original, int desconto) {
        long id = produto(nome, "JOGO");
        preco(oferta(id, loja), valor, original, desconto, AGORA.minus(Duration.ofHours(1)));
        return id;
    }

    private long produto(String nome, String tipo) {
        return jdbc.queryForObject("""
                INSERT INTO produto (tipo, nome, ativo) VALUES (?, ?, true) RETURNING id
                """, Long.class, tipo, nome);
    }

    private long oferta(long produtoId, String codigoLoja) {
        String identificador = UUID.randomUUID().toString();
        return jdbc.queryForObject("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url, ativa)
                VALUES (?, (SELECT id FROM loja WHERE codigo = ?), ?, ?, true)
                RETURNING id
                """, Long.class, produtoId, codigoLoja, identificador, "https://loja.test/" + identificador);
    }

    private long ofertaDe(long produtoId) {
        return jdbc.queryForObject("SELECT min(id) FROM oferta WHERE produto_id = ?", Long.class, produtoId);
    }

    private void preco(long ofertaId, long valor, long original, int desconto, Instant coletadoEm) {
        jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, valor_original_centavos, desconto_pct, disponivel,
                                   origem_coleta, coletado_em)
                VALUES (?, ?, ?, ?, true, 'ITAD_API', ?)
                """, ofertaId, valor, original, desconto, Timestamp.from(coletadoEm));
    }
}
