package dev.davidcaetano.umbra_api.config;

import dev.davidcaetano.umbra_api.comum.WireMockIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class SecurityConfigTest extends WireMockIntegrationTestBase {

    private static final String ROTA_PROTEGIDA = "/rota-protegida-inexistente";
    private static final String TIPO_NAO_AUTENTICADO = "https://umbra.dev/erros/nao-autenticado";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private JwtEncoder jwtEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void montarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void rotaProtegidaSemToken_deveResponder401EmProblemDetailComDesafioBearer() throws Exception {
        mockMvc.perform(get(ROTA_PROTEGIDA))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.type").value(TIPO_NAO_AUTENTICADO))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value(ROTA_PROTEGIDA));
    }

    @Test
    void tokenValido_devePassarPeloFiltroEChegarAoRoteamento() throws Exception {
        comToken(get(ROTA_PROTEGIDA), token(SecurityConfig.EMISSOR_DO_TOKEN, daquiAUmaHora()))
                .andExpect(status().isNotFound());
    }

    @Test
    void tokenExpirado_deveResponder401() throws Exception {
        comToken(get(ROTA_PROTEGIDA), token(SecurityConfig.EMISSOR_DO_TOKEN, haDoisMinutos()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(TIPO_NAO_AUTENTICADO));
    }

    @Test
    void tokenDeOutroEmissor_deveResponder401() throws Exception {
        comToken(get(ROTA_PROTEGIDA), token("outro-emissor", daquiAUmaHora()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenComPayloadAdulterado_deveResponder401() throws Exception {
        String[] partes = token(SecurityConfig.EMISSOR_DO_TOKEN, daquiAUmaHora()).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        String payloadAdulterado = base64Url(payload.replace("\"sub\":\"1\"", "\"sub\":\"2\""));

        comToken(get(ROTA_PROTEGIDA), partes[0] + "." + payloadAdulterado + "." + partes[2])
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAssinadoComOutraChave_deveResponder401() throws Exception {
        var outraChave = new SecretKeySpec("x".repeat(32).getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoderDeOutraChave = NimbusJwtEncoder.withSecretKey(outraChave).algorithm(MacAlgorithm.HS256).build();

        comToken(get(ROTA_PROTEGIDA), token(encoderDeOutraChave, SecurityConfig.EMISSOR_DO_TOKEN, daquiAUmaHora()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSemAssinatura_deveResponder401() throws Exception {
        String cabecalho = base64Url("{\"alg\":\"none\"}");
        String payload = base64Url("{\"sub\":\"1\",\"iss\":\"umbra-api\",\"exp\":4102444800}");

        comToken(get(ROTA_PROTEGIDA), cabecalho + "." + payload + ".")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenRecusado_naoDeveLevarOTokenAoLog(CapturedOutput saida) throws Exception {
        String tokenExpirado = token(SecurityConfig.EMISSOR_DO_TOKEN, haDoisMinutos());

        comToken(get(ROTA_PROTEGIDA), tokenExpirado).andExpect(status().isUnauthorized());

        assertThat(saida.getAll()).doesNotContain(tokenExpirado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/produtos", "/actuator/health", "/actuator/health/readiness", "/v3/api-docs"})
    void rotaPublicaDeLeitura_deveResponderSemToken(String rota) throws Exception {
        mockMvc.perform(get(rota)).andExpect(status().isOk());
    }

    @Test
    void cadastroSemToken_naoDeveSerBarradoPelaSeguranca() throws Exception {
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void escritaEmRotaDeCatalogo_naoDeveSerPublica() throws Exception {
        mockMvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions comToken(MockHttpServletRequestBuilder requisicao, String token) throws Exception {
        return mockMvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String token(String emissor, Instant expiraEm) {
        return token(jwtEncoder, emissor, expiraEm);
    }

    private static String token(JwtEncoder encoder, String emissor, Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emissor)
                .subject("1")
                .issuedAt(expiraEm.minus(1, ChronoUnit.HOURS))
                .expiresAt(expiraEm)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static Instant daquiAUmaHora() {
        return Instant.now().plus(1, ChronoUnit.HOURS);
    }

    private static Instant haDoisMinutos() {
        return Instant.now().minus(2, ChronoUnit.MINUTES);
    }

    private static String base64Url(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }
}
