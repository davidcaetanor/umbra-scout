package dev.davidcaetano.umbra_api.catalogo.controller;

import com.jayway.jsonpath.JsonPath;
import dev.davidcaetano.umbra_api.catalogo.enums.CategoriaHardware;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProdutoControllerTest extends IntegrationTestBase {

    private static final String TIPO_PARAMETRO_INVALIDO = "https://umbra.dev/erros/parametro-invalido";
    private static final String TIPO_RECURSO_NAO_ENCONTRADO = "https://umbra.dev/erros/recurso-nao-encontrado";

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
    void semParametros_deveDevolverAPrimeiraPaginaDoCatalogoAtivoSemExternos() throws Exception {
        produto("t212a-c1 Ativo", "JOGO", null, true);
        produto("t212a-c1 Inativo", "JOGO", null, false);
        Long ativos = jdbc.queryForObject("SELECT count(*) FROM produto WHERE ativo", Long.class);

        mockMvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.totalElementos").value(ativos))
                .andExpect(jsonPath("$.externos").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"t212a-c2 pokemon", "T212A-C2 PoKeMoN", "t212a-c2 pokémon"})
    void buscaPorNome_deveIgnorarAcentoEMaiusculas(String termo) throws Exception {
        long id = produto("t212a-c2 Pokémon Scarlet", "JOGO", null, true);

        mockMvc.perform(get("/produtos").param("nome", termo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].id").value(hasItem((int) id)))
                .andExpect(jsonPath("$.conteudo[*].nome").value(hasItem("t212a-c2 Pokémon Scarlet")));
    }

    @Test
    void buscaPorNome_semTrechoComum_deveDevolverConteudoVazio() throws Exception {
        produto("t212a-c2b Pokémon Scarlet", "JOGO", null, true);

        mockMvc.perform(get("/produtos").param("nome", "t212a-c2b zelda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "abc%", "abc_", "abc\\def"})
    void nomeForaDaRegra_deveDevolver400ComONomeEmErros(String termo) throws Exception {
        esperarParametroInvalido(mockMvc.perform(get("/produtos").param("nome", termo)), "nome");
    }

    @Test
    void nomeCom101Caracteres_deveDevolver400() throws Exception {
        esperarParametroInvalido(mockMvc.perform(get("/produtos").param("nome", "a".repeat(101))), "nome");
    }

    @Test
    void nomeEmBranco_deveSerTratadoComoAusencia() throws Exception {
        mockMvc.perform(get("/produtos").param("nome", "   "))
                .andExpect(status().isOk());
    }

    @Test
    void categoria_deveFiltrar() throws Exception {
        long gpu = produto("t212a-c4 Placa RTX", "HARDWARE", "GPU", true);
        produto("t212a-c4 Processador", "HARDWARE", "CPU", true);

        mockMvc.perform(get("/produtos").param("nome", "t212a-c4").param("categoria", "GPU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(gpu))
                .andExpect(jsonPath("$.conteudo[0].categoria").value("GPU"));
    }

    @Test
    void categoriaInvalida_deveDevolver400ListandoAsDezAceitas() throws Exception {
        ResultActions resposta = mockMvc.perform(get("/produtos").param("categoria", "XYZ"));

        esperarParametroInvalido(resposta, "categoria");
        for (CategoriaHardware categoria : CategoriaHardware.values()) {
            resposta.andExpect(jsonPath("$.erros[0].mensagem").value(containsString(categoria.name())));
        }
    }

    @Test
    void ordenacaoPorMenorPreco_deveDeixarProdutoSemPrecoPorUltimo() throws Exception {
        long esgotado = produto("t212a-c5 A esgotado", "JOGO", null, true);
        preco(oferta(esgotado, "STEAM", true), 5000, null, false, null);
        long caro = produto("t212a-c5 B caro", "JOGO", null, true);
        preco(oferta(caro, "STEAM", true), 30000, null, true, null);
        long barato = produto("t212a-c5 C barato", "JOGO", null, true);
        preco(oferta(barato, "STEAM", true), 10000, null, true, null);

        String json = conteudoDe(mockMvc.perform(get("/produtos")
                .param("nome", "t212a-c5").param("ordenacao", "MENOR_PRECO")));

        assertThat(ids(json)).containsExactly(barato, caro, esgotado);
    }

    @Test
    void ordenacaoInvalida_deveDevolver400() throws Exception {
        esperarParametroInvalido(mockMvc.perform(get("/produtos").param("ordenacao", "XYZ")), "ordenacao");
    }

    @Test
    void sortDoCliente_deveSerIgnoradoSemErro() throws Exception {
        long primeiro = produto("t212a-c6 Alfa", "JOGO", null, true);
        long segundo = produto("t212a-c6 Beta", "JOGO", null, true);

        String json = conteudoDe(mockMvc.perform(get("/produtos")
                .param("nome", "t212a-c6").param("sort", "qualquerCoisa")));

        assertThat(ids(json)).containsExactly(primeiro, segundo);
    }

    @Test
    void tamanhoAcimaDoTeto_deveSerReduzidoPara20() throws Exception {
        for (int i = 0; i < 21; i++) {
            produto("t212a-c7 Item %02d".formatted(i), "JOGO", null, true);
        }

        String json = conteudoDe(mockMvc.perform(get("/produtos").param("nome", "t212a-c7").param("size", "50"))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.totalElementos").value(21)));

        assertThat(ids(json)).hasSize(20);
    }

    @Test
    void produtosDeMesmoNome_devemAparecerUmaVezEntreAsPaginas() throws Exception {
        List<Long> semeados = List.of(
                produto("t212a-c8 Mesmo Nome", "JOGO", null, true),
                produto("t212a-c8 Mesmo Nome", "JOGO", null, true),
                produto("t212a-c8 Mesmo Nome", "JOGO", null, true));

        List<Long> vistos = new ArrayList<>();
        vistos.addAll(ids(conteudoDe(mockMvc.perform(get("/produtos")
                .param("nome", "t212a-c8").param("size", "2").param("page", "0")))));
        vistos.addAll(ids(conteudoDe(mockMvc.perform(get("/produtos")
                .param("nome", "t212a-c8").param("size", "2").param("page", "1")))));

        assertThat(vistos).containsExactlyInAnyOrderElementsOf(semeados);
    }

    @Test
    void produtoInativo_naoAparece_eEsgotadoApareceSemMelhorPreco() throws Exception {
        long inativo = produto("t212a-c9 Inativo", "JOGO", null, false);
        preco(oferta(inativo, "STEAM", true), 1000, null, true, null);
        long esgotado = produto("t212a-c9 Esgotado", "HARDWARE", "SSD", true);
        preco(oferta(esgotado, "KABUM", true), 0, null, false, null);

        String json = conteudoDe(mockMvc.perform(get("/produtos").param("nome", "t212a-c9"))
                .andExpect(jsonPath("$.conteudo[0].melhorPreco").value(nullValue())));

        assertThat(ids(json)).containsExactly(esgotado);
    }

    @Test
    void gratuitoEPromocaoEncerrada_devemSerDecididosNaResposta() throws Exception {
        long gratuito = produto("t212a-c10 A gratuito", "JOGO", null, true);
        preco(oferta(gratuito, "STEAM", true), 0, null, true, null);
        long promocao100 = produto("t212a-c10 B promocao 100", "JOGO", null, true);
        preco(oferta(promocao100, "STEAM", true), 0, 5000L, true, null);
        long encerrada = produto("t212a-c10 C encerrada", "JOGO", null, true);
        preco(oferta(encerrada, "STEAM", true), 2000, 4000L, true, Instant.now().minus(Duration.ofDays(1)));
        long vigente = produto("t212a-c10 D vigente", "JOGO", null, true);
        preco(oferta(vigente, "STEAM", true), 2000, 4000L, true, Instant.now().plus(Duration.ofDays(1)));

        mockMvc.perform(get("/produtos").param("nome", "t212a-c10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].melhorPreco.gratuito").value(true))
                .andExpect(jsonPath("$.conteudo[1].melhorPreco.gratuito").value(false))
                .andExpect(jsonPath("$.conteudo[1].melhorPreco.valorCentavos").value(0))
                .andExpect(jsonPath("$.conteudo[2].melhorPreco.promocaoEncerrada").value(true))
                .andExpect(jsonPath("$.conteudo[2].melhorPreco.expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.conteudo[3].melhorPreco.promocaoEncerrada").value(false));
    }

    @Test
    void detalhe_deveTrazerSoOfertasDisponiveisEmOrdemDePreco() throws Exception {
        long id = produto("t212a-c11 Detalhe", "JOGO", null, true);
        preco(oferta(id, "STEAM", true), 9000, null, true, null);
        preco(oferta(id, "NUUVEM", true), 7000, 9000L, true, null);
        preco(oferta(id, "GOG", true), 5000, null, false, null);

        mockMvc.perform(get("/produtos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nome").value("t212a-c11 Detalhe"))
                .andExpect(jsonPath("$.ofertas.length()").value(2))
                .andExpect(jsonPath("$.ofertas[0].loja").value("Nuuvem"))
                .andExpect(jsonPath("$.ofertas[0].valorCentavos").value(7000))
                .andExpect(jsonPath("$.ofertas[1].loja").value("Steam"))
                .andExpect(jsonPath("$.melhorPreco.loja").value("Nuuvem"))
                .andExpect(jsonPath("$.melhorPreco.valorCentavos").value(7000));
    }

    @Test
    void empateDeValor_deveDarAMesmaLojaNaListagemENoDetalhe() throws Exception {
        long id = produto("t212a-c12 Empate", "JOGO", null, true);
        preco(oferta(id, "STEAM", true), 5000, null, true, null);
        preco(oferta(id, "NUUVEM", true), 5000, null, true, null);

        String lojaDaListagem = JsonPath.read(conteudoDe(mockMvc.perform(get("/produtos")
                .param("nome", "t212a-c12"))), "$.conteudo[0].melhorPreco.loja");

        mockMvc.perform(get("/produtos/{id}", id))
                .andExpect(jsonPath("$.ofertas[0].loja").value(lojaDaListagem))
                .andExpect(jsonPath("$.melhorPreco.loja").value(lojaDaListagem));
        assertThat(lojaDaListagem).isEqualTo("Steam");
    }

    @Test
    void produtoInexistente_deveDevolver404EmProblemDetail() throws Exception {
        long inexistente = jdbc.queryForObject("SELECT coalesce(max(id), 0) + 1000 FROM produto", Long.class);

        esperarNaoEncontrado(mockMvc.perform(get("/produtos/{id}", inexistente)), "/produtos/" + inexistente);
    }

    @Test
    void produtoInativo_deveDevolver404EmProblemDetail() throws Exception {
        long inativo = produto("t212a-c13 Inativo", "JOGO", null, false);

        esperarNaoEncontrado(mockMvc.perform(get("/produtos/{id}", inativo)), "/produtos/" + inativo);
    }

    @Test
    void idQueNaoENumero_deveDevolver400EmProblemDetail() throws Exception {
        esperarParametroInvalido(mockMvc.perform(get("/produtos/abc")), "id")
                .andExpect(jsonPath("$.instance").value("/produtos/abc"));
    }

    @Test
    void produtoSemOfertaDisponivel_deveTrazerOfertasVaziaEMelhorPrecoNulo() throws Exception {
        long id = produto("t212a-c15 Sem oferta", "HARDWARE", "HD", true);
        preco(oferta(id, "KABUM", true), 0, null, false, null);

        mockMvc.perform(get("/produtos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ofertas").isEmpty())
                .andExpect(jsonPath("$.melhorPreco").value(nullValue()));
    }

    @Test
    void respostas_naoDevemExporCampoInterno() throws Exception {
        long id = jdbc.queryForObject("""
                INSERT INTO produto (tipo, nome, chave_itad) VALUES ('JOGO', 't212a-c16 Interno', ?) RETURNING id
                """, Long.class, UUID.randomUUID().toString());
        preco(oferta(id, "STEAM", true), 1000, null, true, null);

        String listagem = conteudoDe(mockMvc.perform(get("/produtos").param("nome", "t212a-c16")));
        String detalhe = conteudoDe(mockMvc.perform(get("/produtos/{id}", id)));

        for (String json : List.of(listagem, detalhe)) {
            assertThat(json).doesNotContain("chaveItad", "\"ativo\"", "\"ativa\"", "origemColeta",
                    "criadoEm", "atualizadoEm", "ofertaId");
        }
    }

    @Test
    void incluirExternoSemNome_deveDevolver400() throws Exception {
        mockMvc.perform(get("/produtos").param("incluirExterno", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(TIPO_PARAMETRO_INVALIDO));
    }

    @Test
    void documentacaoOpenApi_deveCitarAsRotasDeProduto() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/produtos']").exists())
                .andExpect(jsonPath("$.paths['/produtos/{id}']").exists())
                .andExpect(jsonPath("$.paths['/produtos/{id}/historico']").exists());
    }

    private ResultActions esperarParametroInvalido(ResultActions resposta, String campo) throws Exception {
        return resposta
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(TIPO_PARAMETRO_INVALIDO))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem(campo)));
    }

    private void esperarNaoEncontrado(ResultActions resposta, String caminho) throws Exception {
        resposta.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(TIPO_RECURSO_NAO_ENCONTRADO))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value(caminho));
    }

    private static String conteudoDe(ResultActions resposta) throws Exception {
        return resposta.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static List<Long> ids(String json) {
        List<Number> ids = JsonPath.read(json, "$.conteudo[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    private long produto(String nome, String tipo, String categoria, boolean ativo) {
        return jdbc.queryForObject("""
                INSERT INTO produto (tipo, nome, categoria, ativo) VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, tipo, nome, categoria, ativo);
    }

    private long oferta(long produtoId, String codigoLoja, boolean ativa) {
        String identificador = UUID.randomUUID().toString();
        return jdbc.queryForObject("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url, ativa)
                VALUES (?, (SELECT id FROM loja WHERE codigo = ?), ?, ?, ?)
                RETURNING id
                """, Long.class, produtoId, codigoLoja, identificador, "https://loja.test/" + identificador, ativa);
    }

    private void preco(long ofertaId, long valorCentavos, Long valorOriginalCentavos, boolean disponivel,
                       Instant expiraEm) {
        jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, valor_original_centavos, disponivel, origem_coleta,
                                   expira_em, coletado_em)
                VALUES (?, ?, ?, ?, 'ITAD_API', ?, ?)
                """, ofertaId, valorCentavos, valorOriginalCentavos, disponivel,
                expiraEm == null ? null : Timestamp.from(expiraEm),
                Timestamp.from(Instant.now().minus(Duration.ofHours(1))));
    }
}
