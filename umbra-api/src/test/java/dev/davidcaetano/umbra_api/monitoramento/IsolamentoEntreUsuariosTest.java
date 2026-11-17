package dev.davidcaetano.umbra_api.monitoramento;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import dev.davidcaetano.umbra_api.usuario.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Evidência do RNF-03. Toda rota protegida precisa de um caso de isolamento declarado em
 * {@link #CASOS_DE_ISOLAMENTO}; rota nova sem caso faz este teste falhar.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IsolamentoEntreUsuariosTest extends IntegrationTestBase {

    private static final String QUALQUER_METODO = "*";

    private static final Set<String> ROTAS_PUBLICAS = Set.of(
            "POST /auth/cadastro",
            "POST /auth/login",
            "POST /auth/oauth2/token",
            "GET /produtos",
            "GET /produtos/{id}",
            "GET /produtos/{id}/historico",
            "* /error",
            "GET /v3/api-docs",
            "GET /v3/api-docs.yaml",
            "GET /v3/api-docs/swagger-config",
            "GET /swagger-ui.html");

    private static final Map<String, String> CASOS_DE_ISOLAMENTO = Map.of(
            "POST /monitoramentos", "criar_mesmoProdutoDoOutroUsuarioDeveSerAceitoSemRevelarOMonitoramentoAlheio",
            "GET /monitoramentos", "listar_naoDeveMostrarMonitoramentoDoOutroUsuario",
            "PATCH /monitoramentos/{id}", "alterar_monitoramentoAlheioDeveResponderOMesmo404DeUmInexistenteESemEfeito",
            "DELETE /monitoramentos/{id}", "apagar_monitoramentoAlheioDeveResponderOMesmo404DeUmInexistenteESemEfeito",
            "GET /usuarios/eu", "eu_deveDevolverSomenteAContaDoToken");

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeamentos;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private Long dono;
    private Long intruso;
    private Long produto;
    private Long monitoramentoDoDono;

    @BeforeEach
    void prepararDoisUsuarios() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        dono = inserirUsuario("dono@umbra.dev");
        intruso = inserirUsuario("intruso@umbra.dev");
        produto = jdbc.queryForObject("INSERT INTO produto (tipo, nome) VALUES ('JOGO', 'Disputado') RETURNING id",
                Long.class);
        como(dono, post("/monitoramentos").content(corpoDeCriacao(produto, 1000))).andExpect(status().isCreated());
        monitoramentoDoDono = jdbc.queryForObject("SELECT id FROM monitoramento WHERE usuario_id = ?",
                Long.class, dono);
    }

    @Test
    void todaRotaProtegidaDeveTerCasoDeIsolamentoDeclaradoEExistente() {
        Set<String> protegidas = new TreeSet<>(rotasMapeadas());
        protegidas.removeAll(ROTAS_PUBLICAS);

        assertThat(protegidas)
                .as("rota protegida sem caso de isolamento, ou caso declarado para rota que não existe")
                .isEqualTo(new TreeSet<>(CASOS_DE_ISOLAMENTO.keySet()));
        assertThat(rotasMapeadas()).as("rota pública declarada que não existe").containsAll(ROTAS_PUBLICAS);

        Set<String> testes = Arrays.stream(getClass().getDeclaredMethods())
                .filter(metodo -> metodo.isAnnotationPresent(Test.class))
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertThat(testes).containsAll(CASOS_DE_ISOLAMENTO.values());
    }

    @Test
    void todaRotaProtegidaDeveResponder401SemTokenOuComTokenInvalido() throws Exception {
        for (String rota : CASOS_DE_ISOLAMENTO.keySet()) {
            assertThat(mockMvc.perform(requisicaoPara(rota)).andReturn().getResponse().getStatus())
                    .as("sem token: " + rota).isEqualTo(401);
            assertThat(mockMvc.perform(requisicaoPara(rota).header(HttpHeaders.AUTHORIZATION, "Bearer invalido"))
                    .andReturn().getResponse().getStatus())
                    .as("token inválido: " + rota).isEqualTo(401);
        }
    }

    @Test
    void todaRotaPublicaDeveResponderSemToken() throws Exception {
        for (String rota : ROTAS_PUBLICAS) {
            assertThat(mockMvc.perform(requisicaoPara(rota)).andReturn().getResponse().getStatus())
                    .as("rota pública exigiu token: " + rota).isNotEqualTo(401);
        }
    }

    @Test
    void criar_mesmoProdutoDoOutroUsuarioDeveSerAceitoSemRevelarOMonitoramentoAlheio() throws Exception {
        como(intruso, post("/monitoramentos").content(corpoDeCriacao(produto, 5000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precoAlvoCentavos").value(5000));

        assertThat(jdbc.queryForObject("SELECT preco_alvo_centavos FROM monitoramento WHERE id = ?",
                Long.class, monitoramentoDoDono)).isEqualTo(1000);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM monitoramento WHERE produto_id = ?",
                Integer.class, produto)).isEqualTo(2);
    }

    @Test
    void listar_naoDeveMostrarMonitoramentoDoOutroUsuario() throws Exception {
        como(intruso, get("/monitoramentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.conteudo").isEmpty());
        como(dono, get("/monitoramentos"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(monitoramentoDoDono));
    }

    @Test
    void alterar_monitoramentoAlheioDeveResponderOMesmo404DeUmInexistenteESemEfeito() throws Exception {
        String rota = "/monitoramentos/" + monitoramentoDoDono;
        String corpo = "{\"ativo\": false, \"precoAlvoCentavos\": 1}";

        String alheio = corpoDe404(como(intruso, patch(rota).content(corpo)));
        assertThat(jdbc.queryForMap("SELECT ativo, preco_alvo_centavos FROM monitoramento WHERE id = ?",
                monitoramentoDoDono)).containsEntry("ativo", true).containsEntry("preco_alvo_centavos", 1000L);

        como(dono, delete(rota)).andExpect(status().isNoContent());
        assertThat(corpoDe404(como(intruso, patch(rota).content(corpo)))).isEqualTo(alheio);
    }

    @Test
    void alterar_corpoInvalidoDeveResponderOMesmo400ParaDonoEIntruso() throws Exception {
        String rota = "/monitoramentos/" + monitoramentoDoDono;
        String invalido = "{\"precoAlvoCentavos\": 0}";

        String doIntruso = corpoDe(como(intruso, patch(rota).content(invalido)).andExpect(status().isBadRequest()));
        String doDono = corpoDe(como(dono, patch(rota).content(invalido)).andExpect(status().isBadRequest()));

        assertThat(doIntruso).isEqualTo(doDono);
    }

    @Test
    void apagar_monitoramentoAlheioDeveResponderOMesmo404DeUmInexistenteESemEfeito() throws Exception {
        String rota = "/monitoramentos/" + monitoramentoDoDono;

        String alheio = corpoDe404(como(intruso, delete(rota)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM monitoramento WHERE id = ?",
                Integer.class, monitoramentoDoDono)).isEqualTo(1);

        como(dono, delete(rota)).andExpect(status().isNoContent());
        assertThat(corpoDe404(como(intruso, delete(rota)))).isEqualTo(alheio);
    }

    @Test
    void eu_deveDevolverSomenteAContaDoToken() throws Exception {
        como(dono, get("/usuarios/eu"))
                .andExpect(jsonPath("$.id").value(dono))
                .andExpect(jsonPath("$.email").value("dono@umbra.dev"));
        como(intruso, get("/usuarios/eu"))
                .andExpect(jsonPath("$.id").value(intruso))
                .andExpect(jsonPath("$.email").value("intruso@umbra.dev"));
    }

    private Set<String> rotasMapeadas() {
        return mapeamentos.getHandlerMethods().keySet().stream()
                .flatMap(info -> {
                    Set<RequestMethod> metodos = info.getMethodsCondition().getMethods();
                    Set<String> nomes = metodos.isEmpty()
                            ? Set.of(QUALQUER_METODO)
                            : metodos.stream().map(RequestMethod::name).collect(Collectors.toSet());
                    return info.getPatternValues().stream()
                            .flatMap(caminho -> nomes.stream().map(metodo -> metodo + " " + caminho));
                })
                .collect(Collectors.toSet());
    }

    private static MockHttpServletRequestBuilder requisicaoPara(String rota) {
        String[] partes = rota.split(" ", 2);
        String metodo = QUALQUER_METODO.equals(partes[0]) ? "GET" : partes[0];
        String caminho = partes[1].replaceAll("\\{[^}]+}", "1");
        return request(HttpMethod.valueOf(metodo), caminho).contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    private Long inserirUsuario(String email) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (email, senha_hash, nome_exibicao)
                VALUES (?, 'hash', 'Pessoa')
                RETURNING id
                """, Long.class, email);
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

    private static String corpoDe404(ResultActions resposta) throws Exception {
        return corpoDe(resposta.andExpect(status().isNotFound()));
    }

    private static String corpoDe(ResultActions resposta) {
        MvcResult resultado = resposta.andReturn();
        return new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }
}
