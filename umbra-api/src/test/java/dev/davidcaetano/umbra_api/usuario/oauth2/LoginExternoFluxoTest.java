package dev.davidcaetano.umbra_api.usuario.oauth2;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.JsonPath;
import dev.davidcaetano.umbra_api.comum.WireMockIntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class LoginExternoFluxoTest extends WireMockIntegrationTestBase {

    private static final String RETORNO_FRONT = "http://localhost:4200/login/callback";
    private static final String ACCESS_TOKEN_DO_DISCORD = "access-token-do-discord-de-teste";
    private static final String EMAIL = "fluxo-discord@exemplo.com";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        jdbc.update("DELETE FROM usuario WHERE email = ?", EMAIL);
        WIREMOCK.stubFor(WireMock.post(urlEqualTo("/api/oauth2/token")).willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                        {"access_token": "%s", "token_type": "Bearer", "expires_in": 604800,
                         "scope": "identify email"}
                        """.formatted(ACCESS_TOKEN_DO_DISCORD))));
    }

    @Test
    void loginComDiscord_deveRedirecionarComCodigoQueVaiUmaVezPeloJwt(CapturedOutput saida) throws Exception {
        perfilDoDiscord(true);

        MvcResult callback = concluirLoginNoDiscord();

        String destino = callback.getResponse().getRedirectedUrl();
        assertThat(destino).startsWith(RETORNO_FRONT + "?codigo=");
        String codigo = parametro(destino, "codigo");

        String token = JsonPath.read(trocar(codigo).andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn().getResponse().getContentAsString(), "$.token");

        mockMvc.perform(get("/usuarios/eu").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
        trocar(codigo).andExpect(status().isUnauthorized());
        assertThat(saida.getAll()).doesNotContain(codigo, token, ACCESS_TOKEN_DO_DISCORD);
    }

    @Test
    void callback_naoDeveDeixarSessaoNemCookieDeSessaoVivos() throws Exception {
        perfilDoDiscord(true);
        MockHttpSession sessao = new MockHttpSession();

        MvcResult callback = concluirLoginNoDiscord(sessao);

        assertThat(sessao.isInvalid()).isTrue();
        Cookie cookie = callback.getResponse().getCookie("JSESSIONID");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getValue()).isEmpty();
        assertThat(callback.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .allSatisfy(cabecalho -> assertThat(cabecalho).startsWith("JSESSIONID=;").contains("Max-Age=0"));
    }

    @Test
    void emailNaoVerificadoNoDiscord_deveVoltarAoFrontComErroSemCodigo() throws Exception {
        perfilDoDiscord(false);

        MvcResult callback = concluirLoginNoDiscord();

        assertThat(callback.getResponse().getRedirectedUrl()).isEqualTo(RETORNO_FRONT + "?erro=email_nao_verificado");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = ?", Long.class, EMAIL)).isZero();
    }

    @Test
    void usuarioQueCancelaNoDiscord_deveVoltarAoFrontComEntradaCancelada() throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        String state = iniciarLogin(sessao);

        MvcResult callback = mockMvc.perform(get("/login/oauth2/code/discord")
                        .param("error", "access_denied").param("state", state).session(sessao))
                .andExpect(status().isFound())
                .andReturn();

        assertThat(callback.getResponse().getRedirectedUrl()).isEqualTo(RETORNO_FRONT + "?erro=entrada_cancelada");
        assertThat(sessao.isInvalid()).isTrue();
    }

    @Test
    void callbackSemPedidoDeAutorizacao_deveVoltarAoFrontComErro() throws Exception {
        MvcResult callback = mockMvc.perform(get("/login/oauth2/code/discord")
                        .param("code", "codigo-forjado").param("state", "state-forjado"))
                .andExpect(status().isFound())
                .andReturn();

        assertThat(callback.getResponse().getRedirectedUrl()).isEqualTo(RETORNO_FRONT + "?erro=falha_no_provedor");
    }

    @Test
    void inicioDoLogin_deveRedirecionarAoDiscordComState() throws Exception {
        MvcResult inicio = mockMvc.perform(get("/oauth2/authorization/discord")).andExpect(status().isFound())
                .andReturn();

        assertThat(inicio.getResponse().getRedirectedUrl())
                .startsWith("https://discord.com/oauth2/authorize?")
                .contains("response_type=code", "client_id=discord-cliente-de-teste", "scope=identify%20email",
                        "state=");
    }

    @Test
    void inicioDoLoginComGoogle_deveRedirecionarAoGoogle() throws Exception {
        MvcResult inicio = mockMvc.perform(get("/oauth2/authorization/google")).andExpect(status().isFound())
                .andReturn();

        assertThat(inicio.getResponse().getRedirectedUrl())
                .startsWith("https://accounts.google.com/o/oauth2/v2/auth?")
                .contains("client_id=google-cliente-de-teste", "scope=openid%20email%20profile");
    }

    @Test
    void paginaDeLoginPadraoDoSpring_naoDeveExistir() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isUnauthorized());
    }

    @Test
    void trocaComCodigoEmBranco_deveResponder400() throws Exception {
        mockMvc.perform(post("/auth/oauth2/token").contentType(MediaType.APPLICATION_JSON).content("{\"codigo\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    private MvcResult concluirLoginNoDiscord() throws Exception {
        return concluirLoginNoDiscord(new MockHttpSession());
    }

    private MvcResult concluirLoginNoDiscord(MockHttpSession sessao) throws Exception {
        String state = iniciarLogin(sessao);
        return mockMvc.perform(get("/login/oauth2/code/discord")
                        .param("code", "codigo-do-discord").param("state", state).session(sessao))
                .andExpect(status().isFound())
                .andReturn();
    }

    private String iniciarLogin(MockHttpSession sessao) throws Exception {
        String destino = mockMvc.perform(get("/oauth2/authorization/discord").session(sessao))
                .andExpect(status().isFound())
                .andReturn().getResponse().getRedirectedUrl();
        return parametro(destino, "state");
    }

    private ResultActions trocar(String codigo) throws Exception {
        return mockMvc.perform(post("/auth/oauth2/token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\": \"%s\"}".formatted(codigo)));
    }

    private static String parametro(String url, String nome) {
        String valor = UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst(nome);
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }

    private static void perfilDoDiscord(boolean verificado) {
        WIREMOCK.stubFor(WireMock.get(urlEqualTo("/api/users/@me"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + ACCESS_TOKEN_DO_DISCORD))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"id": "80351110224678912", "username": "fluxo", "global_name": "Fluxo",
                                 "email": "%s", "verified": %s}
                                """.formatted(EMAIL, verificado))));
    }
}
