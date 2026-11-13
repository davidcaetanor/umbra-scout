package dev.davidcaetano.umbra_api.usuario.controller;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthControllerTest extends IntegrationTestBase {

    private static final String EMAIL = "usuario_xanisvaldo@exemplo.com";
    private static final String SENHA = "senha-forte-do-teste";
    private static final String TIPO_PARAMETRO_INVALIDO = "https://umbra.dev/erros/parametro-invalido";
    private static final String TIPO_RECURSO_EM_CONFLITO = "https://umbra.dev/erros/recurso-em-conflito";

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
    void cadastroValido_deveResponder201ComOUsuarioSemSenhaNemHash() throws Exception {
        String corpo = cadastrar("Usuario_Xanisvaldo@Exemplo.com", SENHA, "Xanisvaldo")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.nomeExibicao").value("Xanisvaldo"))
                .andExpect(jsonPath("$.criadoEm").isString())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String hash = jdbc.queryForObject("SELECT senha_hash FROM usuario WHERE email = ?", String.class, EMAIL);
        assertThat(hash).startsWith("$2a$12$");
        assertThat(corpo).doesNotContain(SENHA).doesNotContain(hash);
    }

    @Test
    void emailDuplicado_deveResponder409EmProblemDetail() throws Exception {
        cadastrar(EMAIL, SENHA, "Xanisvaldo").andExpect(status().isCreated());

        cadastrar("USUARIO_XANISVALDO@exemplo.com", "outra-senha-forte", "Outro")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(TIPO_RECURSO_EM_CONFLITO))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.instance").value("/auth/cadastro"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario", Long.class)).isEqualTo(1);
    }

    @Test
    void senhaFraca_deveResponder400SemEcoarASenhaENaoGravar() throws Exception {
        String corpo = cadastrar(EMAIL, "curta12", "Xanisvaldo")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(TIPO_PARAMETRO_INVALIDO))
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("senha")))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).doesNotContain("curta12");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario", Long.class)).isZero();
    }

    @Test
    void senhaAcimaDoLimiteDoBcrypt_deveResponder400EmVezDe500() throws Exception {
        cadastrar(EMAIL, "a".repeat(73), "Xanisvaldo")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("senhaDentroDoLimiteDoBcrypt")));
    }

    @Test
    void emailInvalido_deveResponder400NoCampoEmail() throws Exception {
        cadastrar("sem-arroba", SENHA, "Xanisvaldo")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[*].campo").value(hasItem("email")))
                .andExpect(jsonPath("$.erros[*].campo").value(not(hasItem("senha"))));
    }

    private ResultActions cadastrar(String email, String senha, String nomeExibicao) throws Exception {
        String json = """
                {"email": "%s", "senha": "%s", "nomeExibicao": "%s"}
                """.formatted(email, senha, nomeExibicao);
        return mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
