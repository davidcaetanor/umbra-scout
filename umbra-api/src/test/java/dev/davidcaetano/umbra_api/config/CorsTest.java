package dev.davidcaetano.umbra_api.config;

import dev.davidcaetano.umbra_api.comum.WireMockIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsTest extends WireMockIntegrationTestBase {

    private static final String ORIGEM_DO_FRONTEND = "http://localhost:4200";
    private static final String OUTRA_ORIGEM = "https://site-de-terceiro.exemplo.com";

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc;

    @BeforeEach
    void montarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void preflightDaOrigemDoFrontend_deveLiberarAuthorizationSemExigirToken() throws Exception {
        preflight(ORIGEM_DO_FRONTEND, "/usuarios/eu")
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DO_FRONTEND))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void preflightDeOutraOrigem_deveSerRecusado() throws Exception {
        preflight(OUTRA_ORIGEM, "/usuarios/eu")
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void leituraPublicaDaOrigemDoFrontend_deveExporORequestId() throws Exception {
        mockMvc.perform(get("/produtos").header(HttpHeaders.ORIGIN, ORIGEM_DO_FRONTEND))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DO_FRONTEND))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("X-Request-Id")));
    }

    @Test
    void leituraPublicaDeOutraOrigem_deveSerRecusada() throws Exception {
        mockMvc.perform(get("/produtos").header(HttpHeaders.ORIGIN, OUTRA_ORIGEM))
                .andExpect(status().isForbidden());
    }

    private ResultActions preflight(String origem, String rota) throws Exception {
        return mockMvc.perform(options(rota)
                .header(HttpHeaders.ORIGIN, origem)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"));
    }
}
