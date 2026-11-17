package dev.davidcaetano.umbra_api.config;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import dev.davidcaetano.umbra_api.comum.RotasPublicas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Gera o contrato, publica em {@code target/openapi.json} para o CI e compara com o {@code openapi.json}
 * versionado. Mudança de contrato de propósito: {@code mvn verify -Dcontrato.atualizar=true}.
 */
@SpringBootTest
@ActiveProfiles("test")
class ContratoOpenApiTest extends IntegrationTestBase {

    private static final Path CONTRATO_VERSIONADO = Path.of("openapi.json");
    private static final Path CONTRATO_PUBLICADO = Path.of("target", "openapi.json");
    private static final Set<String> METODOS_HTTP = Set.of("get", "post", "put", "patch", "delete");
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    @Autowired
    private WebApplicationContext contexto;

    private JsonNode contrato;

    @BeforeEach
    void gerarContrato() throws Exception {
        MockHttpServletResponse resposta = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build()
                .perform(get("/v3/api-docs"))
                .andReturn()
                .getResponse();

        assertThat(resposta.getStatus()).isEqualTo(200);
        contrato = JSON.readTree(resposta.getContentAsString(StandardCharsets.UTF_8));
        assertThat(contrato.path("openapi").asString()).startsWith("3.1");
    }

    @Test
    void contratoGeradoDeveSerIgualAoVersionado() throws Exception {
        String formatado = JSON.writeValueAsString(JSON.treeToValue(contrato, Map.class)) + "\n";
        Files.createDirectories(CONTRATO_PUBLICADO.getParent());
        Files.writeString(CONTRATO_PUBLICADO, formatado, StandardCharsets.UTF_8);

        if (Boolean.getBoolean("contrato.atualizar")) {
            Files.writeString(CONTRATO_VERSIONADO, formatado, StandardCharsets.UTF_8);
        }

        assertThat(Files.exists(CONTRATO_VERSIONADO))
                .as("openapi.json ausente; gere com mvn verify -Dcontrato.atualizar=true")
                .isTrue();
        assertThat(contrato)
                .as("contrato mudou sem atualizar o openapi.json; confira o diff e rode mvn verify "
                        + "-Dcontrato.atualizar=true")
                .isEqualTo(JSON.readTree(Files.readString(CONTRATO_VERSIONADO, StandardCharsets.UTF_8)));
    }

    @Test
    void todaOperacaoDeveTerOperationIdProprio() {
        List<String> ids = operacoes().values().stream()
                .map(operacao -> operacao.path("operationId").asString(""))
                .toList();

        assertThat(ids).doesNotContain("").doesNotHaveDuplicates();
    }

    @Test
    void soAsRotasPublicasDevemDispensarOToken() {
        Map<String, JsonNode> comSegurancaPropria = operacoes().entrySet().stream()
                .filter(operacao -> operacao.getValue().has("security"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        comSegurancaPropria.forEach((rota, operacao) ->
                assertThat(operacao.path("security").isEmpty()).as("rota com segurança própria: " + rota).isTrue());
        Set<String> publicas = comSegurancaPropria.keySet();

        assertThat(publicas).isEqualTo(RotasPublicas.documentadasNoContrato());
        assertThat(contrato.path("security").path(0).has(OpenApiConfig.ESQUEMA_BEARER)).isTrue();
    }

    @Test
    void todaRotaProtegidaDeveDeclarar401() {
        operacoes().forEach((rota, operacao) -> {
            if (!operacao.has("security")) {
                assertThat(operacao.path("responses").has("401")).as(rota).isTrue();
            }
        });
    }

    @Test
    void todoCodigoDeErroDaApiDeveEstarDeclaradoEmAlgumaRota() {
        Set<String> declarados = new HashSet<>();
        operacoes().values().forEach(operacao -> operacao.path("responses").propertyNames().forEach(declarados::add));

        assertThat(declarados).contains("400", "401", "404", "409", "422", "500", "503");
        assertThat(contrato.path("components").path("responses").propertyNames())
                .containsExactlyInAnyOrder("ParametroInvalido", "NaoAutenticado", "NaoEncontrado", "EmConflito",
                        "RegraViolada", "ServicoIndisponivel", "ErroInterno");
    }

    @Test
    void respostaDeveDeclararTodosOsCamposEMarcarSoOsQueAceitamNulo() {
        JsonNode monitoramento = contrato.path("components").path("schemas").path("MonitoramentoResponse");

        assertThat(monitoramento.path("required")).hasSize(monitoramento.path("properties").size());
        assertThat(monitoramento.at("/properties/melhorPreco/anyOf/1/type").asString()).isEqualTo("null");
        assertThat(monitoramento.at("/properties/imagemUrl/type").toString()).contains("null");
        assertThat(monitoramento.at("/properties/nome/type").asString()).isEqualTo("string");
    }

    @Test
    void corpoDeRequisicaoNaoDeveExporRegraDeValidacaoComoCampo() {
        JsonNode criar = contrato.path("components").path("schemas").path("CriarMonitoramentoRequest");

        assertThat(criar.path("properties").propertyNames())
                .containsExactlyInAnyOrder("produtoId", "gid", "precoAlvoCentavos");
    }

    @Test
    void contratoNaoDeveFixarEnderecoDeServidor() {
        assertThat(contrato.path("servers")).hasSize(1);
        assertThat(contrato.at("/servers/0/url").asString()).isEqualTo("/");
    }

    private Map<String, JsonNode> operacoes() {
        Map<String, JsonNode> operacoes = new TreeMap<>();
        contrato.path("paths").properties().forEach(caminho -> caminho.getValue().properties().stream()
                .filter(metodo -> METODOS_HTTP.contains(metodo.getKey()))
                .forEach(metodo -> operacoes.put(metodo.getKey().toUpperCase() + " " + caminho.getKey(),
                        metodo.getValue())));
        return operacoes;
    }
}
