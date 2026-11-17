package dev.davidcaetano.umbra_api.config.openapi;

import dev.davidcaetano.umbra_api.comum.openapi.RespostaDeErro;
import dev.davidcaetano.umbra_api.usuario.service.MotivoDeRecusa;
import dev.davidcaetano.umbra_api.usuario.service.ProvedorDeIdentidade;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Regras que valem para toda operação: corpo em JSON, 401 e cadeado em toda rota fora da lista pública
 * e 500 em todas. Rota pública é a que declara {@code @SecurityRequirements} vazio.
 */
public class ContratoDaApiCustomizer implements OpenApiCustomizer {

    private static final String QUALQUER_TIPO = "*/*";
    private static final String JSON = "application/json";
    private static final String TAG_AUTENTICACAO = "Autenticação";

    @Override
    public void customise(OpenAPI openApi) {
        openApi.getPaths().values().forEach(caminho -> caminho.readOperations().forEach(this::aplicarRegrasGerais));
        openApi.getPaths().addPathItem("/oauth2/authorization/{provedor}", inicioDoLoginExterno());
        openApi.getPaths().addPathItem("/login/oauth2/code/{provedor}", retornoDoProvedor());
    }

    private void aplicarRegrasGerais(Operation operacao) {
        if (operacao.getRequestBody() != null) {
            trocarTipoGenerico(operacao.getRequestBody().getContent());
        }
        operacao.getResponses().values().forEach(resposta -> trocarTipoGenerico(resposta.getContent()));

        if (operacao.getSecurity() == null) {
            operacao.getResponses().putIfAbsent("401", referencia(RespostaDeErro.REF_NAO_AUTENTICADO));
        }
        operacao.getResponses().putIfAbsent("500", referencia(RespostaDeErro.REF_ERRO_INTERNO));
    }

    private static void trocarTipoGenerico(Content conteudo) {
        if (conteudo != null && conteudo.containsKey(QUALQUER_TIPO)) {
            conteudo.addMediaType(JSON, conteudo.remove(QUALQUER_TIPO));
        }
    }

    private static PathItem inicioDoLoginExterno() {
        return new PathItem().get(new Operation()
                .operationId("iniciarLoginExterno")
                .tags(List.of(TAG_AUTENTICACAO))
                .summary("Inicia o login pelo provedor externo")
                .description("Navegação do browser, não chamada por fetch: redireciona para o provedor, "
                        + "que devolve o usuário a `/login/oauth2/code/{provedor}`.")
                .security(List.of())
                .addParametersItem(provedor())
                .responses(new ApiResponses()
                        .addApiResponse("302", new ApiResponse().description("Redireciona para o provedor."))));
    }

    private static PathItem retornoDoProvedor() {
        return new PathItem().get(new Operation()
                .operationId("receberRetornoDoProvedor")
                .tags(List.of(TAG_AUTENTICACAO))
                .summary("Retorno do provedor externo")
                .description("Chamada pelo provedor, nunca pelo frontend. Redireciona ao endereço de retorno do "
                        + "frontend com `codigo`, a ser trocado em `POST /auth/oauth2/token`, ou com `erro`, "
                        + "um dos códigos: " + String.join(", ", codigosDeRecusa()) + ".")
                .security(List.of())
                .addParametersItem(provedor())
                .responses(new ApiResponses()
                        .addApiResponse("302", new ApiResponse()
                                .description("Redireciona ao frontend com `codigo` ou `erro`."))));
    }

    private static Parameter provedor() {
        Schema<String> valores = new Schema<String>().types(Set.of("string"));
        Arrays.stream(ProvedorDeIdentidade.values()).map(ProvedorDeIdentidade::idDeRegistro).forEach(valores::addEnumItemObject);
        return new Parameter().name("provedor").in("path").required(true).schema(valores);
    }

    private static List<String> codigosDeRecusa() {
        return Arrays.stream(MotivoDeRecusa.values()).map(MotivoDeRecusa::codigo).toList();
    }

    private static ApiResponse referencia(String ref) {
        return new ApiResponse().$ref(ref);
    }
}
