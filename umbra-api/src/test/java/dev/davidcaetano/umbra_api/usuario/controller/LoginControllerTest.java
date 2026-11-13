package dev.davidcaetano.umbra_api.usuario.controller;

import com.jayway.jsonpath.JsonPath;
import dev.davidcaetano.umbra_api.comum.WireMockIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@ExtendWith(OutputCaptureExtension.class)
class LoginControllerTest extends WireMockIntegrationTestBase {

    private static final String EMAIL = "usuario_xanisvaldo@exemplo.com";
    private static final String SENHA = "senha-forte-do-teste";
    private static final String TIPO_NAO_AUTENTICADO = "https://umbra.dev/erros/nao-autenticado";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    @BeforeEach
    void montarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void loginValido_deveDevolverTokenQueAbreARotaProtegida() throws Exception {
        cadastrar();

        String corpo = entrar("Usuario_Xanisvaldo@Exemplo.com", SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiraEm").isString())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(corpo, "$.token");

        mockMvc.perform(get("/usuarios/eu").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.nomeExibicao").value("Xanisvaldo"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void senhaErradaEEmailDesconhecido_devemResponderO401GenericoIgual() throws Exception {
        cadastrar();

        String senhaErrada = entrar(EMAIL, "senha-errada-do-teste")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(TIPO_NAO_AUTENTICADO))
                .andExpect(jsonPath("$.detail").value("Email ou senha inválidos."))
                .andReturn().getResponse().getContentAsString();
        String emailDesconhecido = entrar("ninguem@exemplo.com", SENHA)
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(emailDesconhecido).isEqualTo(senhaErrada);
    }

    @Test
    void loginSemSenha_deveResponder400NoCampoSenha() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\"}".formatted(EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("senha")));
    }

    @Test
    void senhaAcimaDoLimiteDoBcrypt_deveResponder400EmVezDe500() throws Exception {
        entrar(EMAIL, "a".repeat(73))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("senhaDentroDoLimiteDoBcrypt")));
    }

    @Test
    void loginAceitoERecusado_naoDevemLevarSenhaNemTokenAoLog(CapturedOutput saida) throws Exception {
        cadastrar();

        String corpo = entrar(EMAIL, SENHA).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        entrar(EMAIL, "senha-errada-do-teste").andExpect(status().isUnauthorized());

        assertThat(saida.getAll())
                .doesNotContain(SENHA)
                .doesNotContain("senha-errada-do-teste")
                .doesNotContain(JsonPath.<String>read(corpo, "$.token"));
    }

    @Test
    void usuarioEuSemToken_deveResponder401() throws Exception {
        mockMvc.perform(get("/usuarios/eu"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(TIPO_NAO_AUTENTICADO))
                .andExpect(jsonPath("$.instance").value("/usuarios/eu"));
    }

    @Test
    void tokenDeContaDesativada_deveResponder404EmUsuarioEu() throws Exception {
        cadastrar();
        String corpo = entrar(EMAIL, SENHA).andReturn().getResponse().getContentAsString();
        jdbc.update("UPDATE usuario SET ativo = false WHERE email = ?", EMAIL);

        mockMvc.perform(get("/usuarios/eu")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JsonPath.read(corpo, "$.token")))
                .andExpect(status().isNotFound());
    }

    private void cadastrar() throws Exception {
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "%s", "nomeExibicao": "Xanisvaldo"}
                                """.formatted(EMAIL, SENHA)))
                .andExpect(status().isCreated());
    }

    private ResultActions entrar(String email, String senha) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "senha": "%s"}
                        """.formatted(email, senha)));
    }
}
