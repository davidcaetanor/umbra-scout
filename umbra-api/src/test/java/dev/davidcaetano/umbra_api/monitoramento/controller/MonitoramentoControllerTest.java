package dev.davidcaetano.umbra_api.monitoramento.controller;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import dev.davidcaetano.umbra_api.usuario.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MonitoramentoControllerTest extends IntegrationTestBase {

    private static final String TIPO_PARAMETRO_INVALIDO = "https://umbra.dev/erros/parametro-invalido";
    private static final String TIPO_RECURSO_NAO_ENCONTRADO = "https://umbra.dev/erros/recurso-nao-encontrado";
    private static final String TIPO_RECURSO_EM_CONFLITO = "https://umbra.dev/erros/recurso-em-conflito";
    private static final String TIPO_REGRA_VIOLADA = "https://umbra.dev/erros/regra-de-negocio-violada";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void montarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private Long inserirUsuario(String email) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (email, senha_hash, nome_exibicao)
                VALUES (?, 'hash', 'Pessoa')
                RETURNING id
                """, Long.class, email);
    }

    private Long inserirProduto(String nome) {
        return jdbc.queryForObject("INSERT INTO produto (tipo, nome) VALUES ('JOGO', ?) RETURNING id",
                Long.class, nome);
    }

    private void inserirPrecoAtual(Long produtoId, String identificador, long valor, Long original, Instant expiraEm) {
        Long ofertaId = jdbc.queryForObject("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url)
                VALUES (?, (SELECT id FROM loja WHERE codigo = 'GOG'), ?, 'https://www.gog.com/jogo')
                RETURNING id
                """, Long.class, produtoId, identificador);
        jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, valor_original_centavos, disponivel, origem_coleta,
                                   expira_em, coletado_em)
                VALUES (?, ?, ?, TRUE, 'ITAD_API', ?, now())
                """, ofertaId, valor, original, expiraEm == null ? null : Timestamp.from(expiraEm));
        if (expiraEm != null && expiraEm.isBefore(Instant.now())) {
            jdbc.update("UPDATE oferta SET verificado_em = ? WHERE id = ?",
                    Timestamp.from(expiraEm.minus(2, ChronoUnit.HOURS)), ofertaId);
        }
    }

    private Long precoAvisado(Long monitoramentoId) {
        return jdbc.queryForObject("SELECT preco_avisado_centavos FROM monitoramento WHERE id = ?",
                Long.class, monitoramentoId);
    }

    private ResultActions como(Long usuarioId, MockHttpServletRequestBuilder requisicao) throws Exception {
        return mockMvc.perform(requisicao
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.emitir(usuarioId).valor())
                .contentType(MediaType.APPLICATION_JSON));
    }

    private static String corpoDeCriacao(Long produtoId, long alvo) {
        return """
                {"produtoId": %d, "precoAlvoCentavos": %d}
                """.formatted(produtoId, alvo);
    }

    private Long criar(Long usuarioId, Long produtoId, long alvo) throws Exception {
        como(usuarioId, post("/monitoramentos").content(corpoDeCriacao(produtoId, alvo)))
                .andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT id FROM monitoramento WHERE usuario_id = ? AND produto_id = ?",
                Long.class, usuarioId, produtoId);
    }

    @Test
    void criar_deveResponder201ComLocationMelhorPrecoEDistanciaDoAlvo() throws Exception {
        Long usuario = inserirUsuario("cria@umbra.dev");
        Long produto = inserirProduto("Hollow Knight");
        inserirPrecoAtual(produto, "hk", 4500, 4500L, null);

        como(usuario, post("/monitoramentos").content(corpoDeCriacao(produto, 3000)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*/monitoramentos/\\d+$")))
                .andExpect(jsonPath("$.produtoId").value(produto))
                .andExpect(jsonPath("$.nome").value("Hollow Knight"))
                .andExpect(jsonPath("$.precoAlvoCentavos").value(3000))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.melhorPreco.valorCentavos").value(4500))
                .andExpect(jsonPath("$.melhorPreco.loja").value("GOG"))
                .andExpect(jsonPath("$.distanciaDoAlvoCentavos").value(1500))
                .andExpect(jsonPath("$.alvoAtingido").value(false))
                .andExpect(jsonPath("$.aparentementeDescontinuado").value(false));
    }

    @Test
    void criar_deveMarcarAlvoAtingidoQuandoOMelhorPrecoJaEstiverNoAlvo() throws Exception {
        Long usuario = inserirUsuario("atingido@umbra.dev");
        Long produto = inserirProduto("Celeste");
        inserirPrecoAtual(produto, "ce", 2000, 6000L, null);

        como(usuario, post("/monitoramentos").content(corpoDeCriacao(produto, 2500)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.distanciaDoAlvoCentavos").value(-500))
                .andExpect(jsonPath("$.alvoAtingido").value(true));
    }

    @Test
    void criar_naoDeveMarcarAlvoAtingidoQuandoAPromocaoJaTiverEncerrado() throws Exception {
        Long usuario = inserirUsuario("encerrada@umbra.dev");
        Long produto = inserirProduto("Hades");
        inserirPrecoAtual(produto, "ha", 2000, 6000L, Instant.now().minus(1, ChronoUnit.HOURS));

        como(usuario, post("/monitoramentos").content(corpoDeCriacao(produto, 2500)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.melhorPreco").value(nullValue()))
                .andExpect(jsonPath("$.alvoAtingido").value(false));
    }

    @Test
    void criar_deveAceitarProdutoSemOfertaComMelhorPrecoNulo() throws Exception {
        Long usuario = inserirUsuario("semoferta@umbra.dev");
        Long produto = inserirProduto("Sem Oferta");

        como(usuario, post("/monitoramentos").content(corpoDeCriacao(produto, 2500)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.melhorPreco").doesNotExist())
                .andExpect(jsonPath("$.distanciaDoAlvoCentavos").doesNotExist())
                .andExpect(jsonPath("$.alvoAtingido").value(false));
    }

    @Test
    void criar_deveResponder409QuandoOProdutoJaForMonitorado() throws Exception {
        Long usuario = inserirUsuario("duplo@umbra.dev");
        Long produto = inserirProduto("Duplo");
        criar(usuario, produto, 2500);

        como(usuario, post("/monitoramentos").content(corpoDeCriacao(produto, 1000)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(TIPO_RECURSO_EM_CONFLITO));
    }

    @Test
    void criar_deveResponder404ParaProdutoInexistenteOuInativo() throws Exception {
        Long usuario = inserirUsuario("inexistente@umbra.dev");
        Long inativo = inserirProduto("Inativo");
        jdbc.update("UPDATE produto SET ativo = FALSE WHERE id = ?", inativo);

        como(usuario, post("/monitoramentos").content(corpoDeCriacao(Long.MAX_VALUE, 1000)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(TIPO_RECURSO_NAO_ENCONTRADO));
        como(usuario, post("/monitoramentos").content(corpoDeCriacao(inativo, 1000)))
                .andExpect(status().isNotFound());
    }

    @Test
    void criar_deveResponder422ParaProdutoGratuitoDeVerdade() throws Exception {
        Long usuario = inserirUsuario("gratis@umbra.dev");
        Long produto = inserirProduto("Gratuito");
        inserirPrecoAtual(produto, "gr", 0, 0L, null);

        como(usuario, post("/monitoramentos").content(corpoDeCriacao(produto, 1000)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value(TIPO_REGRA_VIOLADA));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM monitoramento", Integer.class)).isZero();
    }

    @Test
    void criar_deveAceitarPromocaoDeCemPorCento() throws Exception {
        Long usuario = inserirUsuario("cem@umbra.dev");
        Long produto = inserirProduto("Promo Total");
        inserirPrecoAtual(produto, "pt", 0, 5000L, null);

        como(usuario, post("/monitoramentos").content(corpoDeCriacao(produto, 1000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.melhorPreco.gratuito").value(false))
                .andExpect(jsonPath("$.alvoAtingido").value(true));
    }

    @Test
    void criar_deveResponder400ParaAlvoForaDaFaixaOuCampoAusente() throws Exception {
        Long usuario = inserirUsuario("faixa@umbra.dev");
        Long produto = inserirProduto("Faixa");

        como(usuario, post("/monitoramentos").content(corpoDeCriacao(produto, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(TIPO_PARAMETRO_INVALIDO));
        como(usuario, post("/monitoramentos").content(corpoDeCriacao(produto, 100_000_001)))
                .andExpect(status().isBadRequest());
        como(usuario, post("/monitoramentos").content("{\"precoAlvoCentavos\": 1000}"))
                .andExpect(status().isBadRequest());
        como(usuario, post("/monitoramentos").content("{\"produtoId\": %d}".formatted(produto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criar_deveAceitarOsLimitesDaFaixaDoAlvo() throws Exception {
        Long usuario = inserirUsuario("limites@umbra.dev");

        criar(usuario, inserirProduto("Minimo"), 1);
        criar(usuario, inserirProduto("Maximo"), 100_000_000);
    }

    @Test
    void contaDesativada_deveReceber404EmTodasAsRotasMesmoComTokenValido() throws Exception {
        Long usuario = inserirUsuario("desativada@umbra.dev");
        Long produto = inserirProduto("Desativada");
        Long monitoramento = criar(usuario, produto, 1000);
        jdbc.update("UPDATE usuario SET ativo = FALSE WHERE id = ?", usuario);

        como(usuario, post("/monitoramentos").content(corpoDeCriacao(inserirProduto("Outro"), 1000)))
                .andExpect(status().isNotFound());
        como(usuario, get("/monitoramentos")).andExpect(status().isNotFound());
        como(usuario, patch("/monitoramentos/" + monitoramento).content("{\"ativo\": false}"))
                .andExpect(status().isNotFound());
        como(usuario, delete("/monitoramentos/" + monitoramento)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM monitoramento WHERE usuario_id = ?",
                Integer.class, usuario)).isEqualTo(1);
    }


    @Test
    void listar_deveDevolverAPaginaDoUsuarioComOrdemFixaMesmoComSortDoCliente() throws Exception {
        Long usuario = inserirUsuario("lista@umbra.dev");
        Long outro = inserirUsuario("outro@umbra.dev");
        criar(usuario, inserirProduto("Primeiro"), 1000);
        Long segundo = criar(usuario, inserirProduto("Segundo"), 1000);
        criar(outro, inserirProduto("Alheio"), 1000);

        como(usuario, get("/monitoramentos").param("size", "1").param("sort", "precoAlvoCentavos,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(segundo))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(1))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void listar_deveLimitarOTamanhoDaPaginaA20() throws Exception {
        Long usuario = inserirUsuario("teto@umbra.dev");

        como(usuario, get("/monitoramentos").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho").value(20));
    }

    @Test
    void listar_comProdutoIdDeveDevolverSomenteOMonitoramentoDaqueleProduto() throws Exception {
        Long usuario = inserirUsuario("filtro@umbra.dev");
        Long procurado = inserirProduto("Procurado");
        Long monitorado = criar(usuario, procurado, 1500);
        criar(usuario, inserirProduto("Outro"), 1000);

        como(usuario, get("/monitoramentos").param("produtoId", procurado.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(monitorado))
                .andExpect(jsonPath("$.conteudo[0].produtoId").value(procurado))
                .andExpect(jsonPath("$.conteudo[0].precoAlvoCentavos").value(1500))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void listar_comProdutoIdNaoMonitoradoDeveDevolverPaginaVazia() throws Exception {
        Long usuario = inserirUsuario("vazio@umbra.dev");
        criar(usuario, inserirProduto("Monitorado"), 1000);

        como(usuario, get("/monitoramentos").param("produtoId", inserirProduto("Livre").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void listar_deveResponder400ParaProdutoIdInvalido() throws Exception {
        Long usuario = inserirUsuario("invalido@umbra.dev");

        for (String invalido : new String[] {"0", "-3", "abc"}) {
            como(usuario, get("/monitoramentos").param("produtoId", invalido))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void alterar_devePausarEReativarSemMudarOAlvo() throws Exception {
        Long usuario = inserirUsuario("pausa@umbra.dev");
        Long monitoramento = criar(usuario, inserirProduto("Pausa"), 1000);

        como(usuario, patch("/monitoramentos/" + monitoramento).content("{\"ativo\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.precoAlvoCentavos").value(1000));
        como(usuario, patch("/monitoramentos/" + monitoramento).content("{\"ativo\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void alterar_deveTrocarOAlvoSemTocarNoUltimoAviso() throws Exception {
        Long usuario = inserirUsuario("alvo@umbra.dev");
        Long monitoramento = criar(usuario, inserirProduto("Alvo"), 1000);
        jdbc.update("UPDATE monitoramento SET ultimo_aviso_em = now() - INTERVAL '1 hour' WHERE id = ?",
                monitoramento);

        como(usuario, patch("/monitoramentos/" + monitoramento).content("{\"precoAlvoCentavos\": 800}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precoAlvoCentavos").value(800));
        assertThat(jdbc.queryForObject("SELECT ultimo_aviso_em IS NOT NULL FROM monitoramento WHERE id = ?",
                Boolean.class, monitoramento)).isTrue();
    }

    @Test
    void alterar_trocarOAlvoDeveReabrirOAvisoMantendoOTeto() throws Exception {
        Long usuario = inserirUsuario("reabre@umbra.dev");
        Long monitoramento = criar(usuario, inserirProduto("Reabre"), 1000);
        jdbc.update("""
                UPDATE monitoramento SET ultimo_aviso_em = now() - INTERVAL '1 hour', preco_avisado_centavos = 900
                 WHERE id = ?
                """, monitoramento);

        como(usuario, patch("/monitoramentos/" + monitoramento).content("{\"precoAlvoCentavos\": 1200}"))
                .andExpect(status().isOk());

        assertThat(precoAvisado(monitoramento)).isNull();
        assertThat(jdbc.queryForObject("SELECT ultimo_aviso_em IS NOT NULL FROM monitoramento WHERE id = ?",
                Boolean.class, monitoramento)).isTrue();
    }

    @Test
    void alterar_mesmoAlvoOuPausaNaoDevemReabrirOAviso() throws Exception {
        Long usuario = inserirUsuario("naoreabre@umbra.dev");
        Long monitoramento = criar(usuario, inserirProduto("Nao Reabre"), 1000);
        jdbc.update("UPDATE monitoramento SET preco_avisado_centavos = 900 WHERE id = ?", monitoramento);

        como(usuario, patch("/monitoramentos/" + monitoramento).content("{\"precoAlvoCentavos\": 1000}"))
                .andExpect(status().isOk());
        como(usuario, patch("/monitoramentos/" + monitoramento).content("{\"ativo\": false}"))
                .andExpect(status().isOk());
        como(usuario, patch("/monitoramentos/" + monitoramento).content("{\"ativo\": true}"))
                .andExpect(status().isOk());

        assertThat(precoAvisado(monitoramento)).isEqualTo(900L);
    }

    @Test
    void alterar_deveResponder422AoDarAlvoAProdutoQueFicouGratuito() throws Exception {
        Long usuario = inserirUsuario("ficougratis@umbra.dev");
        Long produto = inserirProduto("Ficou Gratis");
        Long monitoramento = criar(usuario, produto, 1000);
        inserirPrecoAtual(produto, "fg", 0, null, null);

        como(usuario, patch("/monitoramentos/" + monitoramento).content("{\"precoAlvoCentavos\": 500}"))
                .andExpect(status().isUnprocessableContent());
        como(usuario, patch("/monitoramentos/" + monitoramento).content("{\"ativo\": false}"))
                .andExpect(status().isOk());
    }

    @Test
    void alterar_deveResponder400ComCorpoVazio() throws Exception {
        Long usuario = inserirUsuario("vazio@umbra.dev");
        Long monitoramento = criar(usuario, inserirProduto("Vazio"), 1000);

        como(usuario, patch("/monitoramentos/" + monitoramento).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(TIPO_PARAMETRO_INVALIDO));
    }

    @Test
    void apagar_deveResponder204EDepois404() throws Exception {
        Long usuario = inserirUsuario("apaga@umbra.dev");
        Long monitoramento = criar(usuario, inserirProduto("Apaga"), 1000);

        como(usuario, delete("/monitoramentos/" + monitoramento)).andExpect(status().isNoContent());
        como(usuario, delete("/monitoramentos/" + monitoramento)).andExpect(status().isNotFound());
    }
}
